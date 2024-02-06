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
import com.tangem.crypto.encodeToBase58String
import java.math.BigDecimal

/**
 * @author Anton Zhilenkov on 01.08.2023.
 */
class NearWalletManager(
    wallet: Wallet,
    private val networkService: NearNetworkService,
    private val txBuilder: NearTransactionBuilder,
) : WalletManager(wallet), TransactionSender {

    override val currentHost: String
        get() = networkService.host

    override suspend fun updateInternal() {
        val walletInfoResult = networkService.getAccount(wallet.address)
        val protocolConfigResult = networkService.getProtocolConfig().successOr { return }
        when (walletInfoResult) {
            is Result.Success -> {
                when (val account = walletInfoResult.data) {
                    is NearAccount.Full -> updateWallet(
                        amountValue = account.near.value,
                        depositValue = account.storageUsage.value *
                            protocolConfigResult.runtimeConfig.storageAmountPerByte,
                    )

                    NearAccount.NotInitialized -> {
                        updateError(BlockchainSdkError.AccountNotFound)
                        return
                    }
                }
            }

            is Result.Failure -> updateError(walletInfoResult.error)
        }

        updateTransactions()
    }

    private fun updateWallet(amountValue: BigDecimal, depositValue: BigDecimal) {
        if (amountValue >= depositValue) {
            val realAmount = amountValue - depositValue
            wallet.setAmount(Amount(realAmount, wallet.blockchain))
            wallet.setReserveValue(NearAmount.DEPOSIT_VALUE)
        } else {
            wallet.setReserveValue(amountValue)
            wallet.setAmount(Amount(BigDecimal.ZERO, wallet.blockchain))
        }
    }

    private suspend fun updateTransactions() {
        wallet.recentTransactions.firstOrNull()?.let {
            val status = networkService.getStatus(requireNotNull(it.hash), it.sourceAddress).successOr { return }
            if (status.isSuccessful) {
                it.status = TransactionStatus.Confirmed
            }
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val destinationAccount = networkService.getAccount(destination).successOr { return it }
        val protocolConfig = networkService.getProtocolConfig().successOr { return it }
        val gasPrice = networkService.getGas(blockHash = null).successOr { return it }

        val isImplicitAccount = destination.length == IMPLICIT_ACCOUNT_ADDRESS_LENGTH

        return when (destinationAccount) {
            is NearAccount.Full -> {
                val feeYocto = protocolConfig.calculateSendFundsFee(gasPrice, isImplicitAccount)
                val feeAmount = Amount(NearAmount(feeYocto).value, wallet.blockchain)
                Result.Success(TransactionFee.Single(Fee.Common(feeAmount)))
            }

            NearAccount.NotInitialized -> {
                val feeYocto = protocolConfig.calculateSendFundsFee(gasPrice, isImplicitAccount) +
                    protocolConfig.calculateCreateAccountFee(gasPrice)
                val feeAmount = Amount(NearAmount(feeYocto).value, wallet.blockchain)
                Result.Success(TransactionFee.Single(Fee.Common(feeAmount)))
            }
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val accessKey =
            networkService.getAccessKey(wallet.address, wallet.publicKey.blockchainKey.encodeToBase58String())
                .successOr { return it.toSimpleFailure() }

        val txToSign = txBuilder.buildForSign(
            transaction = transactionData,
            nonce = accessKey.nextNonce,
            blockHash = accessKey.blockHash,
        )

        return when (val signatureResult = signer.sign(txToSign, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val txToSend = txBuilder.buildForSend(
                    transaction = transactionData,
                    signature = signatureResult.data,
                    nonce = accessKey.nextNonce,
                    blockHash = accessKey.blockHash,
                )
                when (val sendResultHash = networkService.sendTransaction(txToSend.encodeBase64NoWrap())) {
                    is Result.Success -> {
                        transactionData.hash = sendResultHash.data
                        wallet.addOutgoingTransaction(transactionData = transactionData, hashToLowercase = false)

                        SimpleResult.Success
                    }

                    is Result.Failure -> {
                        sendResultHash.toSimpleFailure()
                    }
                }
            }

            is CompletionResult.Failure -> {
                SimpleResult.Failure(signatureResult.error.toBlockchainSdkError())
            }
        }
    }

    suspend fun validateAddress(address: String): Boolean {
        // implicit address validation
        if (address.length == IMPLICIT_ACCOUNT_ADDRESS_LENGTH && hexRegex.matches(address)) {
            return wallet.blockchain.validateAddress(address)
        }

        // named address validation
        if (address.length in MIN_ACCOUNT_ADDRESS_LENGTH until IMPLICIT_ACCOUNT_ADDRESS_LENGTH &&
            nearAddressRegex.matches(address)
        ) {
            val result = getAccount(address)
            return result is Result.Success && result.data is NearAccount.Full
        }

        return false
    }

    suspend fun getAccount(address: String): Result<NearAccount> {
        return networkService.getAccount(address)
    }

    companion object {
        private const val IMPLICIT_ACCOUNT_ADDRESS_LENGTH = 64
        private const val MIN_ACCOUNT_ADDRESS_LENGTH = 2
        private val hexRegex = Regex("^[0-9a-f]+$")
        private val nearAddressRegex = Regex("^(([a-z\\d]+[\\-_])*[a-z\\d]+\\.)*([a-z\\d]+[\\-_])*[a-z\\d]+\$")
    }
}
