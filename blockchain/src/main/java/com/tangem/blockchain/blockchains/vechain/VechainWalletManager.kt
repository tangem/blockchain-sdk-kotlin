package com.tangem.blockchain.blockchains.vechain

import android.util.Log
import com.tangem.blockchain.blockchains.vechain.network.VechainNetworkProvider
import com.tangem.blockchain.blockchains.vechain.network.VechainNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.extensions.toSimpleFailure
import com.tangem.common.CompletionResult

internal class VechainWalletManager(
    wallet: Wallet,
    networkProviders: List<VechainNetworkProvider>,
) : WalletManager(wallet), TransactionSender {

    private val networkService = VechainNetworkService(
        networkProviders = networkProviders,
        blockchain = wallet.blockchain,
    )

    override val currentHost: String get() = networkService.host

    private val transactionBuilder = VechainTransactionBuilder(
        blockchain = wallet.blockchain,
        publicKey = wallet.publicKey,
    )

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

    private fun updateWallet(info: VechainAccountInfo) {
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
            contractAddress = "0x0000000000000000000000000000456E65726779",
            decimals = 18,
        )
    }
}