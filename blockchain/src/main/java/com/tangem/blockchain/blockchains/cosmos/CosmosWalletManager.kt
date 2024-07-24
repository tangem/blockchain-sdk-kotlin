package com.tangem.blockchain.blockchains.cosmos

import android.util.Log
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.blockchains.cosmos.network.CosmosNetworkService
import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.crypto.CryptoUtils
import java.math.BigDecimal
import java.math.RoundingMode

class CosmosWalletManager(
    wallet: Wallet,
    networkProviders: List<CosmosRestProvider>,
    private val cosmosChain: CosmosChain,
) : WalletManager(wallet), TransactionSender {

    private val networkService: CosmosNetworkService = CosmosNetworkService(
        providers = networkProviders,
        cosmosChain = cosmosChain,
    )
    private var accountNumber: Long? = null
    private var sequenceNumber: Long = 0L

    private val txBuilder: CosmosTransactionBuilder = CosmosTransactionBuilder(
        cosmosChain = cosmosChain,
        publicKey = wallet.publicKey,
    )

    private var gas: BigDecimal? = null

    override val currentHost: String
        get() = networkService.host

    override val allowsFeeSelection: FeeSelectionState = cosmosChain.allowsFeeSelection
    override suspend fun updateInternal() {
        val unconfirmedTxHashes = wallet.recentTransactions
            .filter { it.status == TransactionStatus.Unconfirmed }
            .mapNotNull { it.hash }
        when (val accountResult = networkService.getAccountInfo(wallet.address, cardTokens, unconfirmedTxHashes)) {
            is Result.Failure -> updateError(accountResult.error)
            is Result.Success -> updateWallet(accountResult.data)
        }
    }

    // TODO think about split base "send" method to "sign" and "send" to satisfy SRP
    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val accNumber = accountNumber ?: return Result.Failure(BlockchainSdkError.AccountNotFound())

        val hash = when (transactionData) {
            is TransactionData.Uncompiled -> {
                txBuilder.buildForSign(
                    amount = transactionData.amount,
                    source = wallet.address,
                    destination = transactionData.destinationAddress,
                    accountNumber = accNumber,
                    sequenceNumber = sequenceNumber,
                    feeAmount = transactionData.fee?.amount,
                    gas = gas?.toLong(),
                    extras = transactionData.extras as? CosmosTransactionExtras,
                )
            }
            is TransactionData.Compiled -> {
                txBuilder.buildForSign(
                    value = transactionData.value,
                    accountNumber = accNumber,
                    sequenceNumber = sequenceNumber,
                )
            }
        }

        val message = when (val signature = signer.sign(hash, wallet.publicKey)) {
            is CompletionResult.Success -> {
                when (transactionData) {
                    is TransactionData.Uncompiled -> {
                        txBuilder.buildForSend(
                            amount = transactionData.amount,
                            source = wallet.address,
                            destination = transactionData.destinationAddress,
                            accountNumber = accNumber,
                            sequenceNumber = sequenceNumber,
                            feeAmount = transactionData.fee?.amount,
                            gas = gas?.toLong(),
                            extras = transactionData.extras as? CosmosTransactionExtras,
                            signature = signature.data,
                        )
                    }
                    is TransactionData.Compiled -> {
                        txBuilder.buildForSend(
                            value = transactionData.value,
                            accountNumber = accNumber,
                            sequenceNumber = sequenceNumber,
                            signature = signature.data,
                        )
                    }
                }
            }

            is CompletionResult.Failure -> {
                return Result.fromTangemSdkError(signature.error)
            }
        }

        return sendToNetwork(transactionData, message)
    }

    private suspend fun sendToNetwork(
        transactionData: TransactionData,
        message: String,
    ): Result<TransactionSendResult> {
        return when (val sendResult = networkService.send(message)) {
            is Result.Failure -> sendResult
            is Result.Success -> {
                val hash = sendResult.data

                val transaction = when (transactionData) {
                    is TransactionData.Uncompiled -> {
                        transactionData.copy(
                            hash = hash,
                            status = TransactionStatus.Unconfirmed,
                            sourceAddress = wallet.address,
                        )
                    }
                    is TransactionData.Compiled -> {
                        transactionData.copy(
                            hash = hash,
                            status = TransactionStatus.Unconfirmed,
                        )
                    }
                }
                transactionData.hash = hash
                wallet.addOutgoingTransaction(transaction)
                Result.Success(TransactionSendResult(hash))
            }
        }
    }

    @Suppress("MagicNumber")
    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> = try {
        val accNumber = accountNumber ?: throw BlockchainSdkError.FailedToLoadFee

        val input = txBuilder.buildForSend(
            amount = amount,
            source = wallet.address,
            destination = destination,
            accountNumber = accNumber,
            sequenceNumber = sequenceNumber,
            feeAmount = Amount(amount, BigDecimal.ZERO),
            gas = 0,
            extras = null,
            signature = CryptoUtils.generateRandomBytes(length = 64), // signature is not necessary for fee calculation
        )

        when (val estimateGasResult = networkService.estimateGas(input)) {
            is Result.Failure -> estimateGasResult
            is Result.Success -> {
                val amounts = cosmosChain.gasPrices(amount.type).map { gasPrice ->
                    val estimatedGas = estimateGasResult.data
                    val gasMultiplier = cosmosChain.gasMultiplier
                    val feeMultiplier = cosmosChain.feeMultiplier
                    val gas = BigDecimal(estimatedGas) * gasMultiplier
                    this.gas = gas
                    var feeValue = (gas * gasPrice * feeMultiplier)
                        .movePointLeft(wallet.blockchain.decimals())
                        .setScale(wallet.blockchain.decimals(), RoundingMode.DOWN)
                    tax(amount)?.let { feeValue += it }
                    cosmosChain.getExtraFee(amount)?.let { feeValue += it }

                    when (wallet.blockchain) {
                        Blockchain.TerraV1 -> Amount(value = feeValue, amount = amount)
                        else -> Amount(value = feeValue, blockchain = wallet.blockchain)
                    }
                }

                when (amounts.size) {
                    1 -> {
                        Result.Success(TransactionFee.Single(Fee.Common(amounts[0])))
                    }

                    3 -> {
                        Result.Success(
                            TransactionFee.Choosable(
                                minimum = Fee.Common(amounts[0]),
                                normal = Fee.Common(amounts[1]),
                                priority = Fee.Common(amounts[2]),
                            ),
                        )
                    }

                    else -> {
                        Result.Failure(BlockchainSdkError.CustomError("Illegal amounts size"))
                    }
                }
            }
        }
    } catch (e: BlockchainSdkError) {
        Result.Failure(e)
    } catch (e: Exception) {
        Result.Failure(e.toBlockchainSdkError())
    }

    private fun updateWallet(cosmosAccountInfo: CosmosAccountInfo) {
        wallet.recentTransactions.forEach {
            if (cosmosAccountInfo.confirmedTransactionHashes.contains(it.hash)) it.status = TransactionStatus.Confirmed
        }
        wallet.setAmount(cosmosAccountInfo.amount)
        accountNumber = cosmosAccountInfo.accountNumber
        sequenceNumber = cosmosAccountInfo.sequenceNumber
        cosmosAccountInfo.tokenBalances.forEach { entry ->
            val value = requireNotNull(entry.value.value)
            val token = entry.key
            wallet.addTokenValue(value = value, token = token)
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    private fun tax(amount: Amount): BigDecimal? {
        return when (amount.type) {
            is AmountType.Token -> {
                val taxPercent =
                    cosmosChain.taxRateByContractAddress[amount.type.token.contractAddress] ?: return null
                val amountValue = requireNotNull(amount.value) { "Amount must not be null" }
                return amountValue * taxPercent
            }

            else -> null
        }
    }
}