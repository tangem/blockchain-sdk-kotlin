package com.tangem.blockchain.blockchains.near

import android.util.Log
import com.tangem.blockchain.blockchains.near.network.NearAccount
import com.tangem.blockchain.blockchains.near.network.NearAmount
import com.tangem.blockchain.blockchains.near.network.NearNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.*
import com.tangem.common.CompletionResult
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class NearWalletManager(
    wallet: Wallet,
    private val networkService: NearNetworkService,
    private val txBuilder: NearTransactionBuilder,
) : WalletManager(wallet), TransactionSender {

    override val currentHost: String
        get() = networkService.host

    override val dustValue: BigDecimal = NearAmount.DEPOSIT_VALUE

    override suspend fun update() {
        when (val walletInfoResult = networkService.getAccount(wallet.address)) {
            is Result.Success -> {
                when (val account = walletInfoResult.data) {
                    is NearAccount.Full -> updateWallet(account.near.value)
                    NearAccount.Empty -> updateWallet(BigDecimal.ZERO)
                }
            }
            is Result.Failure -> updateError(walletInfoResult.error)
        }
    }

    private fun updateWallet(amountValue: BigDecimal) {
        wallet.setAmount(Amount(amountValue, wallet.blockchain))
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        // val status = networkService.getNetworkStatus()
        //     .successOr { return it }
        // val yoctoGasPrice = networkService.getGas(status.latestBlockHash)
        //     .successOr { return it }.yoctoGasPrice
        val destinationAccount = networkService.getAccount(destination)
            .successOr { return it }

        return when (destinationAccount) {
            is NearAccount.Full -> {
                val feeAmount = Amount(ActionCost.SendFunds.near.value, wallet.blockchain)
                Result.Success(TransactionFee.Single(Fee.Common(feeAmount)))
            }
            NearAccount.Empty -> {
                val cost = ActionCost.CreateAccount.near + ActionCost.SendFunds.near
                val feeAmount = Amount(cost.value, wallet.blockchain)
                Result.Success(TransactionFee.Single(Fee.Common(feeAmount)))
            }
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val accessKey = networkService.getAccessKey(wallet.address).successOr { return it.toSimpleFailure() }
        val sendTo = transactionData.destinationAddress

        return when (val destinationAccountResult = networkService.getAccount(sendTo)) {
            is Result.Success -> {
                val txToSign = txBuilder.buildForSign(transactionData, accessKey.nextNonce, accessKey.blockHash)
                when (val signatureResult = signer.sign(txToSign, wallet.publicKey)) {
                    is CompletionResult.Success -> {
                        val txToSend = txBuilder.buildForSend(
                            transaction = transactionData,
                            signature = signatureResult.data,
                            nonce = accessKey.nextNonce,
                            blockHash = accessKey.blockHash,
                        )
                        when (val sendResult = networkService.sendTransaction(txToSend.encodeBase64NoWrap())) {
                            is Result.Success -> {
                                transactionData.hash = sendResult.data.hash
                                wallet.addOutgoingTransaction(transactionData)
                                SimpleResult.Success
                            }
                            is Result.Failure -> {
                                sendResult.toSimpleFailure()
                            }
                        }
                    }
                    is CompletionResult.Failure -> {
                        SimpleResult.Failure(signatureResult.error.toBlockchainSdkError())
                    }
                }
            }
            is Result.Failure -> {
                destinationAccountResult.toSimpleFailure()
            }
        }
    }
}