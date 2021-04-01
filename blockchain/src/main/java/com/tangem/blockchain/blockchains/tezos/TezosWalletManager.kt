package com.tangem.blockchain.blockchains.tezos

import android.util.Log
import com.tangem.blockchain.blockchains.tezos.network.TezosInfoResponse
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.SignResponse
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.isZero
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.util.*

class TezosWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: TezosTransactionBuilder,
        private val networkProvider: TezosNetworkProvider
) : WalletManager(cardId, wallet), TransactionSender {

    private val blockchain = wallet.blockchain
    private var publicKeyRevealed: Boolean? = null

    override suspend fun update() {
        when (val response = networkProvider.getInfo(wallet.address)) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(response: TezosInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")
        if (response.balance != wallet.amounts[AmountType.Coin]?.value) {
            wallet.recentTransactions.clear()
        }
        wallet.amounts[AmountType.Coin]?.value = response.balance
        transactionBuilder.counter = response.counter
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(
            transactionData: TransactionData, signer: TransactionSigner
    ): Result<SignResponse> {

        if (publicKeyRevealed == null) return Result.Failure(Exception("publicKeyRevealed is null"))

        val contents =
                when (val response =
                        transactionBuilder.buildContents(transactionData, publicKeyRevealed!!)
                ) {
                    is Result.Failure -> return Result.Failure(response.error)
                    is Result.Success -> response.data
                }
        val header =
                when (val response = networkProvider.getHeader()) {
                    is Result.Failure -> return Result.Failure(response.error)
                    is Result.Success -> response.data
                }
        val forgedContents = transactionBuilder.forgeContents(header.hash, contents)
                // potential security vulnerability, transaction should be forged locally
//                when (val response = networkProvider.forgeContents(header.hash, contents)) {
//                    is Result.Failure -> return Result.Failure(response.error)
//                    is Result.Success -> response.data
//                }
        val dataToSign = transactionBuilder.buildToSign(forgedContents)

        val signerResponse = signer.sign(arrayOf(dataToSign), cardId)
        val signature = when (signerResponse) {
            is CompletionResult.Failure -> return Result.failure(signerResponse.error)
            is CompletionResult.Success -> signerResponse.data.signature
        }

        return when (val response = networkProvider.checkTransaction(header, contents, signature)) {
            is SimpleResult.Failure -> response.toResultWithData(signerResponse.data)
            is SimpleResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signature,forgedContents)
                val sendResult = networkProvider.sendTransaction(transactionToSend)

                if (sendResult is SimpleResult.Success) {
                    wallet.addOutgoingTransaction(transactionData)
                }
                sendResult.toResultWithData(signerResponse.data)
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        var fee: BigDecimal = BigDecimal.valueOf(TezosConstants.TRANSACTION_FEE)
        var error: Result.Failure? = null

        coroutineScope {
            val publicKeyRevealedDeferred =
                    async { networkProvider.isPublicKeyRevealed(wallet.address) }
            val destinationInfoDeferred = async { networkProvider.getInfo(destination) }

            when (val result = publicKeyRevealedDeferred.await()) {
                is Result.Failure -> error = result
                is Result.Success -> {
                    publicKeyRevealed = result.data
                    if (!publicKeyRevealed!!) {
                        fee += BigDecimal.valueOf(TezosConstants.REVEAL_FEE)
                    }
                }
            }

            when (val result = destinationInfoDeferred.await()) {
                is Result.Failure -> error = result
                is Result.Success -> {
                    if (result.data.balance.isZero()) {
                        fee += BigDecimal.valueOf(TezosConstants.ALLOCATION_FEE)
                    }
                }
            }
        }
        return if (error == null) Result.Success(listOf(Amount(fee, blockchain))) else error!!
    }

    override fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError> {
        val errors = super.validateTransaction(amount, fee)
        val total = fee?.value?.add(amount.value) ?: amount.value
        if (wallet.amounts[AmountType.Coin]!!.value == total) {
            errors.add(TransactionError.TezosSendAll)
        }
        return errors
    }
}