package com.tangem.blockchain.blockchains.aptos

import android.util.Log
import com.tangem.blockchain.blockchains.aptos.models.AptosAccountInfo
import com.tangem.blockchain.blockchains.aptos.models.AptosTransactionInfo
import com.tangem.blockchain.blockchains.aptos.network.AptosNetworkService
import com.tangem.blockchain.blockchains.aptos.network.response.AptosResource
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import java.math.BigDecimal

internal class AptosWalletManager(
    wallet: Wallet,
    private val networkService: AptosNetworkService,
) : WalletManager(wallet), TransactionSender {

    override val currentHost: String get() = networkService.baseUrl

    private val txBuilder = AptosTransactionBuilder(wallet)
    private var sequenceNumber: Long = 0

    override suspend fun updateInternal() {
        when (val result = networkService.getAccountInfo(wallet.address)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val gasUnitPrice = networkService.getGasUnitPrice()
            .successOr { return it }

        val transaction = txBuilder.buildToCalculateFee(
            sequenceNumber = sequenceNumber,
            destination = destination,
            amount = amount,
            gasUnitPrice = gasUnitPrice,
        )

        val usedGasPriceUnit = networkService.calculateUsedGasPriceUnit(transaction)
            .successOr { return it }

        return Result.Success(
            data = TransactionFee.Single(
                Fee.Aptos(
                    amount = amount.copy(
                        value = BigDecimal(gasUnitPrice * usedGasPriceUnit * SUCCESS_TRANSACTION_SAFE_FACTOR)
                            .movePointLeft(wallet.blockchain.decimals()),
                    ),
                    gasUnitPrice = gasUnitPrice,
                ),
            ),
        )
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val transaction = txBuilder.buildToEncode(sequenceNumber = sequenceNumber, transactionData = transactionData)

        val encodedHash = networkService.encodeTransaction(transaction)
            .successOr { return SimpleResult.Failure(it.error) }

        return when (val signingResult = signer.sign(encodedHash.hexToBytes(), wallet.publicKey)) {
            is CompletionResult.Failure -> SimpleResult.Failure(signingResult.error.toBlockchainSdkError())
            is CompletionResult.Success -> {
                submitTransaction(
                    signedTransaction = transaction,
                    hash = signingResult.data,
                    transactionData = transactionData,
                )
            }
        }
    }

    private fun updateWallet(info: AptosAccountInfo) {
        if (info.sequenceNumber != sequenceNumber) {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        }

        wallet.setCoinValue(value = info.balance.movePointLeft(Blockchain.Aptos.decimals()))
        wallet.updateAptosTokens(info.tokens)

        sequenceNumber = info.sequenceNumber
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    private fun Wallet.updateAptosTokens(tokens: List<AptosResource.TokenResource>) {
        cardTokens
            .mapNotNull { cardToken ->
                val token = tokens.firstOrNull { it.contractAddress == cardToken.contractAddress }
                if (token != null) {
                    token.balance.movePointLeft(cardToken.decimals) to cardToken
                } else {
                    null
                }
            }
            .forEach { addTokenValue(value = it.first, token = it.second) }
    }

    private suspend fun submitTransaction(
        signedTransaction: AptosTransactionInfo,
        hash: ByteArray,
        transactionData: TransactionData,
    ): SimpleResult {
        val transaction = txBuilder.buildToSend(transaction = signedTransaction, hash = hash)

        return when (val result = networkService.submitTransaction(transaction)) {
            is Result.Failure -> SimpleResult.Failure(result.error)
            is Result.Success -> {
                wallet.addOutgoingTransaction(transactionData.copy(hash = result.data))

                SimpleResult.Success
            }
        }
    }

    private companion object {
        const val SUCCESS_TRANSACTION_SAFE_FACTOR = 1.5
    }
}