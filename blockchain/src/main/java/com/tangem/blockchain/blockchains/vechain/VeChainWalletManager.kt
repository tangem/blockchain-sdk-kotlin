package com.tangem.blockchain.blockchains.vechain

import android.util.Log
import com.tangem.blockchain.blockchains.vechain.network.VeChainNetworkProvider
import com.tangem.blockchain.blockchains.vechain.network.VeChainNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.extensions.toSimpleFailure
import com.tangem.common.CompletionResult
import java.math.BigDecimal
import java.util.EnumSet

internal class VeChainWalletManager(
    wallet: Wallet,
    networkProviders: List<VeChainNetworkProvider>,
) : WalletManager(wallet), TransactionSender {

    private val networkService = VeChainNetworkService(
        networkProviders = networkProviders,
        blockchain = wallet.blockchain,
    )

    override val currentHost: String get() = networkService.host

    private val transactionBuilder = VeChainTransactionBuilder(
        blockchain = wallet.blockchain,
        publicKey = wallet.publicKey,
    )

    override fun removeToken(token: Token) {
        if (token == VTHO_TOKEN) return // we don't delete energy token

        super.removeToken(token)
    }

    override fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError> {
        val errors = super.validateTransaction(amount, fee)

        if (amount.type is AmountType.Token && amount.type.token == VTHO_TOKEN) {
            val totalToSend = fee?.value?.add(amount.value) ?: amount.value ?: BigDecimal.ZERO
            val energyBalance = wallet.fundsAvailable(AmountType.Token(VTHO_TOKEN))
            if (energyBalance < totalToSend) {
                errors.add(TransactionError.TotalExceedsBalance)
            }
        }
        return errors
    }

    override suspend fun updateInternal() {
        val pendingTxIds = wallet.recentTransactions.mapNotNullTo(hashSetOf()) { it.hash }
        // Exclude VTHO token, since balance of VTHO fetched with main currency balance
        val tokens = cardTokens
            .filter { !it.contractAddress.equals(VTHO_TOKEN.contractAddress, ignoreCase = true) }
            .toSet()
        when (val response = networkService.getAccountInfo(wallet.address, pendingTxIds, tokens)) {
            is Result.Failure -> updateError(response.error)
            is Result.Success -> updateWallet(response.data)
        }
    }

    private fun updateWallet(info: VeChainAccountInfo) {
        wallet.setAmount(Amount(value = info.balance, blockchain = wallet.blockchain))
        wallet.addTokenValue(value = info.energy, token = VTHO_TOKEN)
        info.tokenBalances.forEach { wallet.addTokenValue(it.value, it.key) }
        info.completedTxIds.forEach { completedTxId ->
            wallet.recentTransactions.find { it.hash == completedTxId }?.let { transactionData ->
                transactionData.status = TransactionStatus.Confirmed
            }
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val vmGas = getVmGas(amount, destination).successOr { return it }

        return Result.Success(transactionBuilder.constructFee(amount, destination, vmGas))
    }

    private suspend fun getVmGas(amount: Amount, destination: String): Result<Long> {
        return when (amount.type) {
            AmountType.Coin -> Result.Success(0)
            is AmountType.Token -> networkService.getVmGas(
                source = wallet.address,
                destination = destination,
                amount = amount,
                token = amount.type.token,
            )
            AmountType.Reserve -> throw BlockchainSdkError.FailedToLoadFee
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val blockInfo = networkService.getLatestBlock()
            .successOr { return SimpleResult.Failure(it.error.toBlockchainSdkError()) }
        val nonce = (0..Long.MAX_VALUE).random()
        val txToSign = transactionBuilder.buildForSign(transactionData, blockInfo, nonce)
        return when (val signatureResult = signer.sign(txToSign, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val rawTx =
                    transactionBuilder.buildForSend(
                        transactionData = transactionData,
                        hash = txToSign,
                        signature = signatureResult.data,
                        blockInfo = blockInfo,
                        nonce = nonce,
                    )
                when (val sendResult = networkService.sendTransaction(rawTx)) {
                    is Result.Success -> {
                        transactionData.hash = sendResult.data.txId
                        wallet.addOutgoingTransaction(transactionData, hashToLowercase = false)

                        SimpleResult.Success
                    }
                    is Result.Failure -> sendResult.toSimpleFailure()
                }
            }
            is CompletionResult.Failure -> SimpleResult.Failure(signatureResult.error.toBlockchainSdkError())
        }
    }

    internal companion object {
        // https://explore.vechain.org/accounts/0x0000000000000000000000000000456e65726779/
        internal val VTHO_TOKEN = Token(
            id = "vethor-token",
            name = "VeThor",
            symbol = "VTHO",
            contractAddress = "0x0000000000000000000000000000456e65726779",
            decimals = 18,
        )
    }
}
