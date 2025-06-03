package com.tangem.blockchain.blockchains.ton

import android.util.Log
import com.tangem.blockchain.blockchains.ton.TonTransactionBuilder.Companion.JETTON_TRANSFER_PROCESSING_FEE
import com.tangem.blockchain.blockchains.ton.models.TonWalletInfo
import com.tangem.blockchain.blockchains.ton.network.TonAccountState
import com.tangem.blockchain.blockchains.ton.network.TonNetworkProvider
import com.tangem.blockchain.blockchains.ton.network.TonNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr

internal class TonWalletManager(
    wallet: Wallet,
    networkProviders: List<TonNetworkProvider>,
) : WalletManager(wallet = wallet), InitializableAccount {

    private var sequenceNumber: Int = 0
    private val txBuilder = TonTransactionBuilder(wallet.publicKey, wallet.address)
    private val networkService = TonNetworkService(
        jsonRpcProviders = networkProviders,
        blockchain = wallet.blockchain,
    )

    override val currentHost: String
        get() = networkService.host

    override var accountInitializationState: InitializableAccount.State = InitializableAccount.State.UNDEFINED

    override suspend fun updateInternal() {
        when (val walletInfoResult = networkService.getWalletInformation(wallet.address, cardTokens)) {
            is Result.Failure -> updateError(walletInfoResult.error)
            is Result.Success -> updateWallet(walletInfoResult.data)
        }
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> = try {
        val txToSign = when (transactionData) {
            is TransactionData.Compiled -> txBuilder.buildCompiledForSign(
                transactionData = transactionData,
                expireAt = createExpirationTimestampSecs(),
            )
            is TransactionData.Uncompiled -> txBuilder.buildForSign(
                sequenceNumber = sequenceNumber,
                amount = transactionData.amount,
                destination = transactionData.destinationAddress,
                extras = transactionData.extras as? TonTransactionExtras,
                expireAt = createExpirationTimestampSecs(),
            )
        }
        val signatureResult = signer.sign(hash = txToSign.hashToSign, publicKey = wallet.publicKey).successOr {
            return Result.fromTangemSdkError(it.error)
        }
        val txToSend = txBuilder.buildForSend(signature = signatureResult, preSignStructure = txToSign)
        when (val sendResult = networkService.send(txToSend)) {
            is Result.Failure -> Result.Failure(sendResult.error)
            is Result.Success -> {
                wallet.addOutgoingTransaction(transactionData.updateHash(sendResult.data))
                Result.Success(TransactionSendResult(sendResult.data))
            }
        }
    } catch (e: BlockchainSdkError) {
        Result.Failure(e)
    } catch (e: Exception) {
        Result.Failure(e.toBlockchainSdkError())
    }

    @Suppress("MagicNumber")
    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> = try {
        val message = txBuilder.buildForSend(
            signature = ByteArray(64),
            sequenceNumber = sequenceNumber,
            amount = amount,
            destination = destination,
            expireAt = createExpirationTimestampSecs(),
            extras = null,
        )

        when (val feeResult = networkService.getFee(wallet.address, message)) {
            is Result.Failure -> feeResult
            is Result.Success -> {
                var fee = feeResult.data
                if (amount.type is AmountType.Token) {
                    fee += JETTON_TRANSFER_PROCESSING_FEE
                }
                Result.Success(TransactionFee.Single(Fee.Common(fee)))
            }
        }
    } catch (e: BlockchainSdkError) {
        Result.Failure(e)
    } catch (e: Exception) {
        Result.Failure(e.toBlockchainSdkError())
    }

    private fun updateWallet(info: TonWalletInfo) {
        accountInitializationState = when (info.accountState) {
            TonAccountState.ACTIVE -> InitializableAccount.State.INITIALIZED
            TonAccountState.UNINITIALIZED -> InitializableAccount.State.NOT_INITIALIZED
        }
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

    @Suppress("MagicNumber")
    private fun createExpirationTimestampSecs(): Int {
        return (System.currentTimeMillis() / 1000 + TRANSACTION_LIFETIME).toInt()
    }

    private companion object {
        const val TRANSACTION_LIFETIME = 60L
    }
}