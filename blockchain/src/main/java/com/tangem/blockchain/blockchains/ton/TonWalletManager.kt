package com.tangem.blockchain.blockchains.ton

import android.util.Log
import com.tangem.blockchain.blockchains.ton.network.TonJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.ton.network.TonNetworkService
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletCoreSigner
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import wallet.core.java.AnySigner
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKeyType
import wallet.core.jni.proto.TheOpenNetwork

class TonWalletManager(
    wallet: Wallet,
    networkProviders: List<TonJsonRpcNetworkProvider>,
) : WalletManager(wallet), TransactionSender {

    private var sequenceNumber: Int = 0
    private val txBuilder = TonTransactionBuilder()
    private val networkService = TonNetworkService(jsonRpcProviders = networkProviders, blockchain = wallet.blockchain)

    override val currentHost: String
        get() = networkService.host

    override suspend fun update() {
        when (val walletInfoResult = networkService.getWalletInformation(wallet.address)) {
            is Result.Failure -> updateError(walletInfoResult.error)
            is Result.Success -> updateWallet(walletInfoResult.data)
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val input = txBuilder.buildForSign(sequenceNumber, transactionData.amount, transactionData.destinationAddress)
        val message = buildTransaction(input, signer).successOr { return SimpleResult.fromTangemSdkError(it.error) }

        return when (val sendResult = networkService.send(message)) {
            is Result.Failure -> SimpleResult.Failure(sendResult.error)
            is Result.Success -> {
                wallet.addOutgoingTransaction(transactionData.copy(hash = sendResult.data))
                SimpleResult.Success
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val input = txBuilder.buildForSign(sequenceNumber, amount, destination)
        val message = buildTransaction(input, null).successOr { return it }

        return when (val feeResult = networkService.getFee(wallet.address, message)) {
            is Result.Failure -> feeResult
            is Result.Success -> Result.Success(listOf(feeResult.data))
        }
    }

    private fun updateWallet(info: TonWalletInfo) {
        if (info.sequenceNumber != sequenceNumber) {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        }

        wallet.setAmount(Amount(value = info.balance, blockchain = wallet.blockchain))
        sequenceNumber = info.sequenceNumber
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    private fun buildTransaction(input: TheOpenNetwork.SigningInput, signer: TransactionSigner?): Result<String> {
        return try {
            val output: TheOpenNetwork.SigningOutput = if (signer != null) {
                val coreSigner = WalletCoreSigner(signer, wallet.publicKey, PublicKeyType.ED25519)
                AnySigner.signExternally(input, CoinType.TON, TheOpenNetwork.SigningOutput.parser(), coreSigner)
            } else {
                AnySigner.sign(input, CoinType.TON, TheOpenNetwork.SigningOutput.parser())
            }

            Result.Success(txBuilder.buildForSend(output))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }
}