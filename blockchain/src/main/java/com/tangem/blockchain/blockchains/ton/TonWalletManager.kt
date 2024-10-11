package com.tangem.blockchain.blockchains.ton

import android.util.Log
import com.tangem.blockchain.blockchains.ton.TonTransactionBuilder.Companion.JETTON_TRANSFER_PROCESSING_FEE
import com.tangem.blockchain.blockchains.ton.models.TonWalletInfo
import com.tangem.blockchain.blockchains.ton.network.TonNetworkProvider
import com.tangem.blockchain.blockchains.ton.network.TonNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKeyType
import wallet.core.jni.proto.TheOpenNetwork

internal class TonWalletManager(
    wallet: Wallet,
    networkProviders: List<TonNetworkProvider>,
) : WalletManager(wallet) {

    private var sequenceNumber: Int = 0
    private val txBuilder = TonTransactionBuilder(wallet.address)
    private val networkService = TonNetworkService(
        jsonRpcProviders = networkProviders,
        blockchain = wallet.blockchain,
    )

    override val currentHost: String
        get() = networkService.host

    override suspend fun updateInternal() {
        when (val walletInfoResult = networkService.getWalletInformation(wallet.address, cardTokens)) {
            is Result.Failure -> updateError(walletInfoResult.error)
            is Result.Success -> updateWallet(walletInfoResult.data)
        }
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        transactionData.requireUncompiled()

        val input = txBuilder.buildForSign(
            sequenceNumber = sequenceNumber,
            amount = transactionData.amount,
            destination = transactionData.destinationAddress,
            extras = transactionData.extras as? TonTransactionExtras,
        ).successOr { return it }

        val message = buildTransaction(input, signer).successOr { return Result.fromTangemSdkError(it.error) }

        return when (val sendResult = networkService.send(message)) {
            is Result.Failure -> Result.Failure(sendResult.error)
            is Result.Success -> {
                wallet.addOutgoingTransaction(transactionData.copy(hash = sendResult.data))
                transactionData.hash = sendResult.data
                Result.Success(TransactionSendResult(sendResult.data))
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val input = txBuilder.buildForSign(sequenceNumber, amount, destination)
            .successOr { return it }
        val message = buildTransaction(input, null).successOr { return it }

        return when (val feeResult = networkService.getFee(wallet.address, message)) {
            is Result.Failure -> feeResult
            is Result.Success -> {
                var fee = feeResult.data
                if (amount.type is AmountType.Token) {
                    fee += JETTON_TRANSFER_PROCESSING_FEE
                }
                Result.Success(TransactionFee.Single(Fee.Common(fee)))
            }
        }
    }

    private fun updateWallet(info: TonWalletInfo) {
        if (info.sequenceNumber != sequenceNumber) {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        }

        wallet.setAmount(Amount(value = info.balance, blockchain = wallet.blockchain))
        sequenceNumber = info.sequenceNumber
        info.jettonDatas.forEach {
            wallet.addTokenValue(it.value.balance, it.key)
        }
        txBuilder.updateJettonAdresses(info.jettonDatas.mapValues { it.value.jettonWalletAddress })
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    private fun buildTransaction(input: TheOpenNetwork.SigningInput, signer: TransactionSigner?): Result<String> {
        val outputResult: Result<TheOpenNetwork.SigningOutput> = AnySignerWrapper().sign(
            walletPublicKey = wallet.publicKey,
            publicKeyType = PublicKeyType.ED25519,
            input = input,
            coin = CoinType.TON,
            parser = TheOpenNetwork.SigningOutput.parser(),
            signer = signer,
            curve = wallet.blockchain.getSupportedCurves().first(),
        )
        return when (outputResult) {
            is Result.Failure -> outputResult
            is Result.Success -> Result.Success(txBuilder.buildForSend(outputResult.data))
        }
    }
}