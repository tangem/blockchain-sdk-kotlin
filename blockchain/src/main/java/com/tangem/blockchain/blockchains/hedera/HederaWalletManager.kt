package com.tangem.blockchain.blockchains.hedera

import android.util.Log
import com.hedera.hashgraph.sdk.AccountBalanceQuery
import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.Client
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.datastorage.BlockchainSavedData
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toCompressedPublicKey
import java.math.BigDecimal
import java.math.RoundingMode

internal class HederaWalletManager(
    wallet: Wallet,
    private val transactionBuilder: HederaTransactionBuilder,
    private val networkProvider: HederaNetworkProvider,
    private val dataStorage: AdvancedDataStorage,
    private val accountCreator: AccountCreator,
) : WalletManager(wallet), TransactionSender {

    private val blockchain = wallet.blockchain
    private val client = if (blockchain.isTestnet()) Client.forTestnet() else Client.forMainnet()

    override val currentHost: String
        get() = networkProvider.baseUrl

    override suspend fun updateInternal() {
        when (val getAccountIdResult = getAccountId()) {
            is Result.Success -> updateBalance(getAccountIdResult.data)
            is Result.Failure -> updateError(getAccountIdResult.error)
        }
    }

    private fun updateBalance(accountId: String) {
        try {
            val balance = AccountBalanceQuery()
                .setAccountId(AccountId.fromString(accountId))
                .execute(client)
            updateWallet(balance.hbars.value)
        } catch (exception: Exception) {
            updateError(exception.toBlockchainSdkError())
        }
    }

    private fun updateWallet(balance: BigDecimal) {
        Log.d(this::class.java.simpleName, "Balance is $balance")

        if (balance != wallet.amounts[AmountType.Coin]?.value) {
            wallet.recentTransactions.clear()
        }
        wallet.changeAmountValue(AmountType.Coin, balance)
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) error("Error isn't BlockchainSdkError")
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        when (val buildTransactionResult = transactionBuilder.buildToSign(transactionData)) {
            is Result.Failure -> return SimpleResult.Failure(buildTransactionResult.error)
            is Result.Success -> {
                val signerResult = signer.sign(buildTransactionResult.data, wallet.publicKey)
                return when (signerResult) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(signerResult.data)
                        try {
                            transactionToSend.execute(client)
                            SimpleResult.Success
                        } catch (exception: Exception) {
                            SimpleResult.Failure(exception.toBlockchainSdkError())
                        }
                    }
                    is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResult.error)
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return when (val usdExchangeRateResult = networkProvider.getUsdExchangeRate()) {
            is Result.Success -> {
                val fee = (HBAR_TRANSFER_USD_COST * MAX_FEE_MULTIPLIER * usdExchangeRateResult.data)
                    .setScale(blockchain.decimals(), RoundingMode.UP)
                Result.Success(TransactionFee.Single(Fee.Common(Amount(fee, blockchain))))
            }
            is Result.Failure -> {
                usdExchangeRateResult
            }
        }
    }

    private suspend fun getAccountId(): Result<String> {
        return if (wallet.address.isBlank()) {
            return when (val getCachedAccountIdResult = getCachedAccountId()) {
                is Result.Success -> {
                    wallet.addresses = setOf(Address(getCachedAccountIdResult.data))

                    getCachedAccountIdResult
                }
                is Result.Failure -> getCachedAccountIdResult
            }
        } else {
            Result.Success(wallet.address)
        }
    }

    private suspend fun getCachedAccountId(): Result<String> {
        val cachedAccountId = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)?.accountId

        return if (cachedAccountId.isNullOrBlank()) {
            return when (val fetchAccountIdResult = fetchAccountId()) {
                is Result.Success -> {
                    dataStorage.store(wallet.publicKey, BlockchainSavedData.Hedera(fetchAccountIdResult.data))

                    fetchAccountIdResult
                }
                is Result.Failure -> fetchAccountIdResult
            }
        } else {
            Result.Success(cachedAccountId)
        }
    }

    private suspend fun fetchAccountId(): Result<String> {
        return when (
            val getAccountIdResult =
                networkProvider.getAccountId(wallet.publicKey.blockchainKey.toCompressedPublicKey())
        ) {
            is Result.Success -> getAccountIdResult
            is Result.Failure -> {
                if (getAccountIdResult.error is BlockchainSdkError.AccountNotFound) {
                    requestCreateAccount()
                } else {
                    getAccountIdResult
                }
            }
        }
    }

    private suspend fun requestCreateAccount(): Result<String> {
        return accountCreator.createAccount(blockchain = blockchain, walletPublicKey = wallet.publicKey.blockchainKey)
    }

    companion object {
        // https://docs.hedera.com/hedera/networks/mainnet/fees
        private val HBAR_TRANSFER_USD_COST = BigDecimal("0.0001")
        // Hedera fees are low, allow 10% safety margin to allow usage of not precise fee estimate
        private val MAX_FEE_MULTIPLIER = BigDecimal("1.1")
    }
}