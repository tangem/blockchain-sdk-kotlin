package com.tangem.blockchain.blockchains.kaspa

import android.util.Log
import com.tangem.blockchain.blockchains.kaspa.krc20.KaspaKRC20InfoResponse
import com.tangem.blockchain.blockchains.kaspa.krc20.KaspaKRC20NetworkProvider
import com.tangem.blockchain.blockchains.kaspa.krc20.KaspaKRC20TransactionExtras
import com.tangem.blockchain.blockchains.kaspa.krc20.model.IncompleteTokenTransactionParams
import com.tangem.blockchain.blockchains.kaspa.krc20.model.RedeemScript
import com.tangem.blockchain.blockchains.kaspa.network.KaspaFeeBucketResponse
import com.tangem.blockchain.blockchains.kaspa.network.KaspaInfoResponse
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.datastorage.BlockchainSavedData
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.trustlines.AssetRequirementsCondition
import com.tangem.blockchain.common.trustlines.AssetRequirementsManager
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.BigInteger

@Suppress("LargeClass")
internal class KaspaWalletManager(
    wallet: Wallet,
    private val transactionBuilder: KaspaTransactionBuilder,
    private val networkProvider: KaspaNetworkProvider,
    private val krc20NetworkProvider: KaspaKRC20NetworkProvider,
    private val dataStorage: AdvancedDataStorage,
) : WalletManager(wallet), UtxoAmountLimitProvider, UtxoBlockchainManager, AssetRequirementsManager {

    override val currentHost: String
        get() = networkProvider.baseUrl

    private val blockchain = wallet.blockchain
    override val dustValue: BigDecimal = BigDecimal("0.2")

    private val dummySigner = DummySigner()

    override val allowConsolidation: Boolean = true

    override suspend fun updateInternal() {
        coroutineScope {
            val coinBalanceDeferred = async { networkProvider.getInfo(wallet.address) }

            val tokensBalances = if (cardTokens.isNotEmpty()) {
                async { krc20NetworkProvider.getBalances(wallet.address, cardTokens.toList()) }.await()
            } else {
                Result.Success(emptyList())
            }

            val coinBalance = coinBalanceDeferred.await()

            if (tokensBalances is Result.Success) {
                updateWalletTokens(tokensBalances.data)
            }

            when (coinBalance) {
                is Result.Success -> updateWallet(coinBalance.data)
                is Result.Failure -> updateError(coinBalance.error)
            }
        }
    }

    private fun updateWallet(response: KaspaInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")
        if (response.balance != wallet.amounts[AmountType.Coin]?.value) {
            // assume outgoing transaction has been finalized if balance has changed
            wallet.recentTransactions.clear()
        }
        wallet.changeAmountValue(AmountType.Coin, response.balance)
        transactionBuilder.unspentOutputs = response.unspentOutputs
    }

    private fun updateWalletTokens(tokensInfo: List<KaspaKRC20InfoResponse>) {
        tokensInfo.forEach { result ->
            val token = result.token
            val balance = result.balance
            wallet.setAmount(balance, amountType = AmountType.Token(token))
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        transactionData.requireUncompiled()

        return when (transactionData.amount.type) {
            is AmountType.Coin -> sendCoinTransaction(transactionData, signer)
            is AmountType.Token -> {
                if (transactionData.extras as? KaspaKRC20TransactionExtras != null) {
                    sendKRC20RevealOnlyTransaction(transactionData, signer)
                } else {
                    sendKRC20Transaction(transactionData, signer)
                }
            }
            else -> error("unknown amount type for fee estimation")
        }
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val unspentOutputCount = transactionBuilder.getUnspentsToSpendCount()

        return if (unspentOutputCount == 0) {
            Result.Failure(Exception("No unspent outputs found").toBlockchainSdkError()) // shouldn't happen
        } else {
            val source = wallet.address

            val transactionData = TransactionData.Uncompiled(
                sourceAddress = source,
                destinationAddress = destination,
                amount = amount,
                fee = null,
            )

            val buildTransactionResult = when (amount.type) {
                is AmountType.Coin -> transactionBuilder.buildToSign(transactionData)
                is AmountType.Token -> transactionBuilder.buildKRC20CommitTransactionToSign(
                    transactionData = transactionData,
                    dust = dustValue,
                ).let {
                    when (it) {
                        is Result.Failure -> it
                        is Result.Success -> Result.Success(it.data.transaction)
                    }
                }
                else -> error("unknown amount type for fee estimation")
            }

            when (buildTransactionResult) {
                is Result.Failure -> return buildTransactionResult
                is Result.Success -> {
                    val transaction = buildTransactionResult.data
                    val hashesToSign = transactionBuilder.getHashesForSign(transaction)
                    return when (val signerResult = dummySigner.sign(hashesToSign, wallet.publicKey)) {
                        is CompletionResult.Success -> {
                            val transactionToSend = transactionBuilder.buildToSend(
                                signatures = signerResult.data.reduce { acc, bytes -> acc + bytes },
                                transaction = transaction,
                            )
                            when (val sendResult = networkProvider.calculateFee(transactionToSend.transaction)) {
                                is Result.Failure -> sendResult
                                is Result.Success -> {
                                    val data = sendResult.data
                                    val mass = when (amount.type) {
                                        is AmountType.Coin -> BigInteger.valueOf(data.mass)
                                        is AmountType.Token -> REVEAL_TRANSACTION_MASS
                                        else -> error("unknown amount type for fee estimation")
                                    }

                                    val allBuckets = (
                                        listOf(data.priorityBucket) +
                                            data.normalBuckets +
                                            data.lowBuckets
                                        ).sortedByDescending { it.feeRate }

                                    Result.Success(
                                        TransactionFee.Choosable(
                                            priority = allBuckets[0].toFee(mass, amount.type),
                                            normal = allBuckets[1].toFee(mass, amount.type),
                                            minimum = allBuckets[2].toFee(mass, amount.type),
                                        ),
                                    )
                                }
                            }
                        }
                        is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
                    }
                }
            }
        }
    }

    override fun checkUtxoAmountLimit(amount: BigDecimal, fee: BigDecimal): UtxoAmountLimit? {
        val unspents = transactionBuilder.getUnspentsToSpend()
        val change = transactionBuilder.calculateChange(amount, fee, unspents)
        return if (change < BigDecimal.ZERO) { // unspentsToSpend not enough to cover transaction amount
            UtxoAmountLimit(
                KaspaTransactionBuilder.MAX_INPUT_COUNT.toBigDecimal(),
                amount + change,
            )
        } else {
            null
        }
    }

    override suspend fun requirementsCondition(currencyType: CryptoCurrencyType): AssetRequirementsCondition? {
        return when (currencyType) {
            is CryptoCurrencyType.Coin -> null
            is CryptoCurrencyType.Token -> {
                getIncompleteTokenTransaction(currencyType.info)?.let {
                    AssetRequirementsCondition.IncompleteTransaction(
                        blockchain = blockchain,
                        amount = Amount(
                            value = it.amountValue,
                            token = currencyType.info,
                        ),
                        feeAmount = Amount(
                            value = it.feeAmountValue,
                            blockchain = blockchain,
                        ),
                    )
                }
            }
        }
    }

    override suspend fun fulfillRequirements(
        currencyType: CryptoCurrencyType,
        signer: TransactionSigner,
    ): SimpleResult {
        return when (currencyType) {
            is CryptoCurrencyType.Coin -> SimpleResult.Success
            is CryptoCurrencyType.Token -> {
                val incompleteTokenTransaction = getIncompleteTokenTransaction(currencyType.info)
                    ?: return SimpleResult.Success

                val result = sendKRC20RevealOnlyTransaction(
                    transactionData = incompleteTokenTransaction
                        .toIncompleteTokenTransactionParams()
                        .toTransactionData(
                            type = AmountType.Token(currencyType.info),
                        ),
                    signer = signer,
                )

                when (result) {
                    is Result.Success -> SimpleResult.Success
                    is Result.Failure -> SimpleResult.Failure(result.error)
                }
            }
        }
    }

    override suspend fun discardRequirements(currencyType: CryptoCurrencyType): SimpleResult {
        when (currencyType) {
            is CryptoCurrencyType.Coin -> Unit
            is CryptoCurrencyType.Token -> {
                removeIncompleteTokenTransaction(currencyType.info)
            }
        }
        return SimpleResult.Success
    }

    private suspend fun sendCoinTransaction(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        when (val buildTransactionResult = transactionBuilder.buildToSign(transactionData)) {
            is Result.Failure -> return buildTransactionResult
            is Result.Success -> {
                val transaction = buildTransactionResult.data
                val hashesToSign = transactionBuilder.getHashesForSign(transaction)
                return when (val signerResult = signer.sign(hashesToSign, wallet.publicKey)) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(
                            signatures = signerResult.data.reduce { acc, bytes -> acc + bytes },
                            transaction = transaction,
                        )
                        when (val sendResult = networkProvider.sendTransaction(transactionToSend)) {
                            is Result.Failure -> sendResult
                            is Result.Success -> {
                                val hash = sendResult.data
                                transactionData.hash = hash
                                wallet.addOutgoingTransaction(transactionData)
                                Result.Success(TransactionSendResult(hash ?: ""))
                            }
                        }
                    }
                    is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
                }
            }
        }
    }

    @Suppress("NestedBlockDepth", "LongMethod")
    private suspend fun sendKRC20Transaction(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        transactionData.requireUncompiled()
        val token = (transactionData.amount.type as AmountType.Token).token
        return when (
            val commitTransaction = transactionBuilder.buildKRC20CommitTransactionToSign(
                transactionData = transactionData,
                dust = dustValue,
            )
        ) {
            is Result.Success -> {
                val revealTransaction = transactionBuilder.buildKRC20RevealTransactionToSign(
                    sourceAddress = transactionData.sourceAddress,
                    redeemScript = commitTransaction.data.redeemScript,
                    feeAmountValue = transactionData.fee?.amount?.value!!,
                    params = commitTransaction.data.params,
                )

                when (revealTransaction) {
                    is Result.Success -> {
                        val unionHashes = commitTransaction.data.hashes + revealTransaction.data.hashes

                        return when (val signerResult = signer.sign(unionHashes, wallet.publicKey)) {
                            is CompletionResult.Success -> {
                                val commitSignaturesLength = commitTransaction.data.hashes.size
                                val commitSignatures = signerResult.data.take(commitSignaturesLength)
                                val revealSignatures = signerResult.data.drop(commitSignaturesLength)
                                val commitTransactionToSend = transactionBuilder.buildToSend(
                                    signatures = commitSignatures.reduce { acc, bytes -> acc + bytes },
                                    transaction = commitTransaction.data.transaction,
                                )
                                val revealTransactionToSend = transactionBuilder.buildKRC20RevealToSend(
                                    signatures = revealSignatures.reduce { acc, bytes -> acc + bytes },
                                    redeemScript = commitTransaction.data.redeemScript,
                                    transaction = revealTransaction.data.transaction,
                                )
                                when (
                                    val sendCommitResult = networkProvider.sendTransaction(commitTransactionToSend)
                                ) {
                                    is Result.Failure -> sendCommitResult
                                    is Result.Success -> {
                                        storeIncompleteTokenTransaction(
                                            token = token,
                                            data = commitTransaction.data
                                                .params
                                                .toBlockchainSavedData(),
                                        )
                                        delay(REVEAL_TRANSACTION_DELAY)

                                        when (
                                            val sendRevealResult = networkProvider.sendTransaction(
                                                revealTransactionToSend,
                                            )
                                        ) {
                                            is Result.Failure -> sendRevealResult
                                            is Result.Success -> {
                                                val hash = sendRevealResult.data
                                                transactionData.hash = hash
                                                wallet.addOutgoingTransaction(transactionData)
                                                removeIncompleteTokenTransaction(token)
                                                Result.Success(TransactionSendResult(hash ?: ""))
                                            }
                                        }
                                    }
                                }
                            }
                            is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
                        }
                    }
                    is Result.Failure -> revealTransaction
                }
            }
            is Result.Failure -> commitTransaction
        }
    }

    private fun IncompleteTokenTransactionParams.toTransactionData(type: AmountType.Token): TransactionData.Uncompiled {
        val token = type.token
        val tokenValue = BigDecimal(envelope.amt)

        val transactionAmount = tokenValue.movePointLeft(token.decimals)
        val fee = feeAmountValue - dustValue
        val feeAmount = Amount(
            value = fee,
            blockchain = blockchain,
        )

        return TransactionData.Uncompiled(
            amount = Amount(
                value = transactionAmount,
                blockchain = blockchain,
                type = type,
            ),
            fee = Fee.Kaspa(
                amount = feeAmount,
                mass = BigInteger.ZERO, // we need only amount
                feeRate = BigInteger.ZERO, // we need only amount
                revealTransactionFee = feeAmount,
            ),
            sourceAddress = wallet.address,
            destinationAddress = envelope.to,
            status = TransactionStatus.Unconfirmed,
            extras = KaspaKRC20TransactionExtras(
                incompleteTokenTransactionParams = this,
            ),
            contractAddress = token.contractAddress,
        )
    }

    private suspend fun sendKRC20RevealOnlyTransaction(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        transactionData.requireUncompiled()
        val token = (transactionData.amount.type as AmountType.Token).token
        val incompleteTokenTransactionParams =
            (transactionData.extras as KaspaKRC20TransactionExtras).incompleteTokenTransactionParams
        val feeAmount = (transactionData.fee as Fee.Kaspa).revealTransactionFee
        val redeemScript = RedeemScript(
            wallet.publicKey.blockchainKey.toCompressedPublicKey(),
            incompleteTokenTransactionParams.envelope,
        )
        val transaction = transactionBuilder.buildKRC20RevealTransactionToSign(
            sourceAddress = transactionData.sourceAddress,
            redeemScript = redeemScript,
            params = incompleteTokenTransactionParams,
            feeAmountValue = feeAmount?.value!!,
        )
        return when (transaction) {
            is Result.Success -> {
                return when (val signerResult = signer.sign(transaction.data.hashes, wallet.publicKey)) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildKRC20RevealToSend(
                            signatures = signerResult.data.reduce { acc, bytes -> acc + bytes },
                            redeemScript = redeemScript,
                            transaction = transaction.data.transaction,
                        )
                        when (val sendResult = networkProvider.sendTransaction(transactionToSend)) {
                            is Result.Failure -> sendResult
                            is Result.Success -> {
                                val hash = sendResult.data
                                transactionData.hash = hash
                                wallet.addOutgoingTransaction(transactionData)
                                removeIncompleteTokenTransaction(token)
                                Result.Success(TransactionSendResult(hash ?: ""))
                            }
                        }
                    }
                    is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
                }
            }
            is Result.Failure -> transaction
        }
    }

    private fun KaspaFeeBucketResponse.toFee(mass: BigInteger, type: AmountType): Fee.Kaspa {
        val feeRate = feeRate.toBigInteger()
        val value = (mass * feeRate).toBigDecimal().movePointLeft(blockchain.decimals())
        return Fee.Kaspa(
            amount = Amount(
                value = value,
                blockchain = blockchain,
                type = type,
            ),
            mass = mass,
            feeRate = feeRate,
            revealTransactionFee = type.takeIf { it is AmountType.Token }.let {
                Amount(
                    value = (mass * feeRate).toBigDecimal().movePointLeft(blockchain.decimals()),
                    blockchain = blockchain,
                    type = type,
                )
            },
        )
    }

    private suspend fun getIncompleteTokenTransaction(
        token: Token,
    ): BlockchainSavedData.KaspaKRC20IncompleteTokenTransaction? {
        return dataStorage.getOrNull(token.createKey())
    }

    private suspend fun storeIncompleteTokenTransaction(
        token: Token,
        data: BlockchainSavedData.KaspaKRC20IncompleteTokenTransaction,
    ) {
        dataStorage.store(token.createKey(), data)
    }

    private suspend fun removeIncompleteTokenTransaction(token: Token) {
        dataStorage.remove(token.createKey())
    }

    private fun Token.createKey(): String {
        return "$symbol-${wallet.publicKey.blockchainKey.toCompressedPublicKey().toHexString()}"
    }

    private fun IncompleteTokenTransactionParams.toBlockchainSavedData() =
        BlockchainSavedData.KaspaKRC20IncompleteTokenTransaction(
            transactionId = transactionId,
            amountValue = amountValue,
            feeAmountValue = feeAmountValue,
            envelope = envelope,
        )

    private fun BlockchainSavedData.KaspaKRC20IncompleteTokenTransaction.toIncompleteTokenTransactionParams() =
        IncompleteTokenTransactionParams(
            transactionId = transactionId,
            amountValue = amountValue,
            feeAmountValue = feeAmountValue,
            envelope = envelope,
        )

    companion object {
        private val REVEAL_TRANSACTION_MASS: BigInteger = 4100.toBigInteger()
        private const val REVEAL_TRANSACTION_DELAY: Long = 2_000
    }
}