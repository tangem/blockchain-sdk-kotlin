package com.tangem.blockchain.blockchains.cardano

import android.util.Log
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.InfoInput
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

internal class CardanoWalletManager(
    wallet: Wallet,
    private val transactionBuilder: CardanoTransactionBuilder,
    private val networkProvider: CardanoNetworkProvider,
) : WalletManager(wallet), TransactionSender, TransactionValidator by transactionBuilder {

    override val dustValue: BigDecimal = BigDecimal.ONE
    override val currentHost: String get() = networkProvider.baseUrl

    private val decimals by lazy { wallet.blockchain.decimals() }

    override suspend fun updateInternal() {
        val input = InfoInput(
            addresses = wallet.addresses.map(Address::value).toSet(),
            tokens = cardTokens,
        )

        when (val response = networkProvider.getInfo(input)) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return try {
            val dummyTransaction = TransactionData.Uncompiled(
                amount = amount,
                fee = null,
                sourceAddress = wallet.address,
                destinationAddress = destination,
            )

            val fee = transactionBuilder.estimateFee(dummyTransaction)

            Result.Success(TransactionFee.Single(fee))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val transactionHash = transactionBuilder.buildForSign(transactionData)

        return when (val signatureResult = signer.sign(transactionHash, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val signatureInfo = SignatureInfo(signatureResult.data, wallet.publicKey.blockchainKey)

                val transactionToSend = transactionBuilder.buildForSend(transactionData, signatureInfo)
                when (val sendResult = networkProvider.sendTransaction(transactionToSend)) {
                    is SimpleResult.Success -> {
                        val hash = transactionHash.toHexString()
                        transactionData.hash = hash
                        wallet.addOutgoingTransaction(transactionData)
                        Result.Success(TransactionSendResult(hash))
                    }
                    is SimpleResult.Failure -> return Result.Failure(sendResult.error)
                }
            }

            is CompletionResult.Failure -> Result.fromTangemSdkError(signatureResult.error)
        }
    }

    private fun updateWallet(response: CardanoAddressResponse) {
        wallet.setCoinValue(value = response.balance.toBigDecimal().movePointLeft(decimals))

        transactionBuilder.update(response.unspentOutputs)

        response.tokenBalances.forEach { tokenAmount ->
            wallet.addTokenValue(
                value = BigDecimal(tokenAmount.value).movePointLeft(tokenAmount.key.decimals),
                token = tokenAmount.key,
            )
        }

        wallet.recentTransactions.forEach { recentTransaction ->
            updateTransactionConfirmation(response, recentTransaction)
        }
    }

    private fun updateTransactionConfirmation(
        response: CardanoAddressResponse,
        recentTransactionData: TransactionData,
    ) {
        // case for Rosetta API, it lacks recent transactions
        val isConfirmed = if (response.recentTransactionsHashes.isEmpty()) {
            response.unspentOutputs.isEmpty() || response.unspentOutputs.hasTransactionHash(recentTransactionData.hash)
        } else {
            // case for APIs with recent transactions
            response.recentTransactionsHashes.containsTransactionHash(recentTransactionData.hash)
        }

        if (isConfirmed) {
            recentTransactionData.status = TransactionStatus.Confirmed
        }
    }

    private fun List<CardanoUnspentOutput>.hasTransactionHash(hash: String?): Boolean {
        return this
            .map { it.transactionHash.toHexString() }
            .containsTransactionHash(hash)
    }

    private fun List<String>.containsTransactionHash(hash: String?): Boolean {
        return any { it.equals(hash, ignoreCase = true) }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }
}