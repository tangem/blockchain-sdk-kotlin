package com.tangem.blockchain.blockchains.kaspa

import android.util.Log
import com.tangem.blockchain.blockchains.kaspa.krc20.KaspaKRC20InfoResponse
import com.tangem.blockchain.blockchains.kaspa.krc20.KaspaKRC20NetworkProvider
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
import com.tangem.blockchain.extensions.*
import com.tangem.blockchain.extensions.map
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
                Result.Success(emptyMap())
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

    private fun updateWalletTokens(tokensInfo: Map<Token, Result<KaspaKRC20InfoResponse>>) {
        tokensInfo.forEach { result ->
            val token = result.key
            val amountType = AmountType.Token(token)
            when (val response = result.value) {
                is Result.Success -> {
                    val balance = response.data.balance
                    wallet.setAmount(balance, amountType)
                }
                is Result.Failure -> {
                    wallet.changeAmountValue(amountType, null, null)
                }
            }
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

        return when (val type = transactionData.amount.type) {
            is AmountType.Coin -> sendCoinTransaction(transactionData, signer)
            is AmountType.Token -> {
                val incompleteTokenTransaction = getIncompleteTokenTransaction(type.token)
                if (incompleteTokenTransaction != null &&
                    incompleteTokenTransaction.amountValue == transactionData.amount.value &&
                    incompleteTokenTransaction.envelope.to == transactionData.destinationAddress
                ) {
                    sendKRC20RevealOnlyTransaction(
                        transactionData = transactionData,
                        signer = signer,
                        incompleteTokenTransactionParams = incompleteTokenTransaction,
                    )
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
                is AmountType.Token -> transactionBuilder.buildToSignKRC20Commit(
                    transactionData = transactionData,
                    dust = dustValue,
                    includeFee = false,
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
                                    val mass = BigInteger.valueOf(data.mass)

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
                    transactionData = incompleteTokenTransaction.toTransactionData(
                        type = AmountType.Token(currencyType.info),
                    ),
                    signer = signer,
                    incompleteTokenTransactionParams = incompleteTokenTransaction,
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
            val commitTransaction = transactionBuilder.buildToSignKRC20Commit(
                transactionData = transactionData,
                dust = dustValue,
            )
        ) {
            is Result.Success -> {
                val revealTransaction = transactionBuilder.buildToSignKRC20Reveal(
                    sourceAddress = transactionData.sourceAddress,
                    redeemScript = commitTransaction.data.redeemScript,
                    revealFeeAmountValue = (transactionData.fee as Fee.Kaspa).revealTransactionFee?.value!!,
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
                                val revealTransactionToSend = transactionBuilder.buildToSendKRC20Reveal(
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
                                            data = commitTransaction.data.params,
                                        )
                                        delay(REVEAL_TRANSACTION_DELAY)

                                        when (
                                            val sendRevealResult = networkProvider.sendTransaction(
                                                revealTransactionToSend,
                                            )
                                        ) {
                                            is Result.Failure -> {
                                                updateUnspentOutputs()
                                                sendRevealResult
                                            }
                                            is Result.Success -> {
                                                updateUnspentOutputs()
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

    private suspend fun sendKRC20RevealOnlyTransaction(
        transactionData: TransactionData,
        signer: TransactionSigner,
        incompleteTokenTransactionParams: BlockchainSavedData.KaspaKRC20IncompleteTokenTransaction,
    ): Result<TransactionSendResult> {
        transactionData.requireUncompiled()

        val token = (transactionData.amount.type as AmountType.Token).token
        val revealFeeAmountValue = (transactionData.fee as Fee.Kaspa).revealTransactionFee?.value ?: BigDecimal.ZERO
        val redeemScript = RedeemScript(
            wallet.publicKey.blockchainKey.toCompressedPublicKey(),
            incompleteTokenTransactionParams.envelope,
        )
        val transaction = transactionBuilder.buildToSignKRC20Reveal(
            sourceAddress = transactionData.sourceAddress,
            redeemScript = redeemScript,
            params = incompleteTokenTransactionParams,
            revealFeeAmountValue = revealFeeAmountValue,
        )
        return when (transaction) {
            is Result.Success -> {
                return when (val signerResult = signer.sign(transaction.data.hashes, wallet.publicKey)) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSendKRC20Reveal(
                            signatures = signerResult.data.reduce { acc, bytes -> acc + bytes },
                            redeemScript = redeemScript,
                            transaction = transaction.data.transaction,
                        )
                        when (val sendResult = networkProvider.sendTransaction(transactionToSend)) {
                            is Result.Failure -> {
                                updateUnspentOutputs()
                                sendResult
                            }
                            is Result.Success -> {
                                updateUnspentOutputs()
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

    private fun BlockchainSavedData.KaspaKRC20IncompleteTokenTransaction.toTransactionData(
        type: AmountType.Token,
    ): TransactionData.Uncompiled {
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
            contractAddress = token.contractAddress,
        )
    }

    private fun KaspaFeeBucketResponse.toFee(mass: BigInteger, type: AmountType): Fee.Kaspa {
        val feeRate = feeRate.toBigInteger()
        val resultMass = if (type is AmountType.Token) {
            mass + REVEAL_TRANSACTION_MASS
        } else {
            mass
        }
        val value = (resultMass * feeRate).toBigDecimal().movePointLeft(blockchain.decimals())
        return Fee.Kaspa(
            amount = Amount(
                value = value,
                blockchain = blockchain,
            ),
            mass = mass,
            feeRate = feeRate,
            revealTransactionFee = type.takeIf { it is AmountType.Token }.let {
                Amount(
                    value = (REVEAL_TRANSACTION_MASS * feeRate).toBigDecimal().movePointLeft(blockchain.decimals()),
                    blockchain = blockchain,
                )
            },
        )
    }

    // we should update unspent outputs as soon as possible before create a new token transaction
    private suspend fun updateUnspentOutputs() {
        coroutineScope {
            networkProvider.getInfo(wallet.address).map {
                transactionBuilder.unspentOutputs = it.unspentOutputs
            }
        }
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

    companion object {
        private val REVEAL_TRANSACTION_MASS: BigInteger = 4100.toBigInteger()
        private const val REVEAL_TRANSACTION_DELAY: Long = 2_000
    }
}