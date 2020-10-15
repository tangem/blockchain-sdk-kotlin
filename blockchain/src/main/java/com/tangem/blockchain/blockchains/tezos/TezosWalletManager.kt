package com.tangem.blockchain.blockchains.tezos

import android.util.Log
import com.tangem.blockchain.blockchains.tezos.network.TezosInfoResponse
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.SignResponse
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.util.*

class TezosWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: TezosTransactionBuilder,
        private val networkManager: TezosNetworkManager
) : WalletManager(cardId, wallet), TransactionSender {

    private val blockchain = wallet.blockchain
    private var publicKeyRevealed: Boolean? = null

    override suspend fun update() {
        val response = networkManager.getInfo(wallet.address)
        when (response) {
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
                when (val response = transactionBuilder.buildContents(transactionData, publicKeyRevealed!!)) {
                    is Result.Failure -> return Result.Failure(response.error)
                    is Result.Success -> response.data
                }
        val header =
                when (val response = networkManager.getHeader()) {
                    is Result.Failure -> return Result.Failure(response.error)
                    is Result.Success -> response.data
                }
        val forgedContents = //TODO: CHANGE FOR PRODUCTION, this is potential security vulnerability, transaction should be forged locally
                when (val response = networkManager.forgeContents(header.hash, contents)) {
                    is Result.Failure -> return Result.Failure(response.error)
                    is Result.Success -> response.data
                }
        val dataToSign = transactionBuilder.buildToSign(forgedContents)

        val signerResponse = signer.sign(arrayOf(dataToSign), cardId)
        val signature = when (signerResponse) {
            is CompletionResult.Failure -> return Result.failure(signerResponse.error)
            is CompletionResult.Success -> signerResponse.data.signature
        }

        return when (val response = networkManager.checkTransaction(header, contents, signature)) {
            is SimpleResult.Failure -> response.toResultWithData(signerResponse.data)
            is SimpleResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signature,forgedContents)
                val sendResult = networkManager.sendTransaction(transactionToSend)

                if (sendResult is SimpleResult.Success) {
                    wallet.addOutgoingTransaction(transactionData)
                }
                sendResult.toResultWithData(signerResponse.data)
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        var fee: BigDecimal = BigDecimal.valueOf(TRANSACTION_FEE)
        var error: Result.Failure? = null

        coroutineScope {
            val publicKeyRevealedDeferred = async { networkManager.isPublicKeyRevealed(wallet.address) }
            val destinationInfoDeferred = async { networkManager.getInfo(destination) }

            when (val result = publicKeyRevealedDeferred.await()) {
                is Result.Failure -> error = result
                is Result.Success -> {
                    publicKeyRevealed = result.data
                    if (!publicKeyRevealed!!) {
                        fee += BigDecimal.valueOf(REVEAL_FEE)
                    }
                }
            }

            when (val result = destinationInfoDeferred.await()) {
                is Result.Failure -> error = result
                is Result.Success -> {
                    if (result.data.balance.isZero()) {
                        fee += BigDecimal.valueOf(ALLOCATION_FEE)
                    }
                }
            }
        }
        return if (error == null) Result.Success(listOf(Amount(fee, blockchain))) else error!!
    }

    override fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError> {
        val errors = super.validateTransaction(amount, fee)
        if (wallet.amounts[AmountType.Coin]!!.value == amount.value!!.add(fee!!.value)) {
            errors.add(TransactionError.TezosSendAll)
        }
        return errors
    }

    companion object {
        const val TRANSACTION_FEE = 0.00142
        const val REVEAL_FEE = 0.0013
        const val ALLOCATION_FEE = 0.257
    }
}