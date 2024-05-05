package com.tangem.blockchain.blockchains.hedera

import android.os.Build
import android.util.Log
import com.hedera.hashgraph.sdk.AccountBalanceQuery
import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.Client
import com.hedera.hashgraph.sdk.TransferTransaction
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.datastorage.BlockchainSavedData
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.*
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.guard
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
            // assume outgoing transaction has been finalized if balance has changed
            wallet.recentTransactions.clear()
        }
        wallet.changeAmountValue(AmountType.Coin, balance)
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) error("Error isn't BlockchainSdkError")
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        return transactionBuilder.buildToSign(transactionData).map { buildTransaction ->
            return when (val signerResult = signer.sign(buildTransaction.signatures, wallet.publicKey)) {
                is CompletionResult.Success -> {
                    val transactionToSend = transactionBuilder.buildToSend(
                        buildTransaction.transferTransaction,
                        signerResult.data,
                    )
                    val executeResult = executeTransaction(transactionToSend)

                    if (executeResult is SimpleResult.Success) {
                        transactionData.setTransactionHash(transactionToSend)
                        wallet.addOutgoingTransaction(transactionData)
                    }
                    executeResult
                }
                is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResult.error)
            }
        }.toSimpleResult()
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return when (val usdExchangeRateResult = networkProvider.getUsdExchangeRate()) {
            is Result.Success -> {
                val isAccountExists = isAccountExist(destination).successOr { false }
                val feeBase = if (isAccountExists) HBAR_TRANSFER_USD_COST else HBAR_CREATE_ACCOUNT_USD_COST
                val fee = (feeBase * MAX_FEE_MULTIPLIER * usdExchangeRateResult.data)
                    .setScale(blockchain.decimals(), RoundingMode.UP)
                Result.Success(TransactionFee.Single(Fee.Common(Amount(fee, blockchain))))
            }
            is Result.Failure -> {
                usdExchangeRateResult
            }
        }
    }

    private fun executeTransaction(transactionToSend: TransferTransaction): SimpleResult {
        return try {
            transactionToSend.execute(client)
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
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

    /** Used to query the status of the `receiving` (`destination`) account. */
    private suspend fun isAccountExist(destinationAddress: String): Result<Boolean> {
        val destinationAccountId = try {
            AccountId.fromString(destinationAddress)
        } catch (e: Exception) {
            return Result.Success(false)
        }
        // Accounts with an account ID and/or EVM address are considered existing accounts
        val accountHasValidAccountIdOrEVMAddress =
            destinationAccountId.num.toInt() != 0 || destinationAccountId.evmAddress != null
        if (accountHasValidAccountIdOrEVMAddress) {
            return Result.Success(true)
        }
        val alias = destinationAccountId.aliasKey.guard { return Result.Success(false) }
        return networkProvider.getAccountId(alias.toBytesRaw()).fold(
            success = { return Result.Success(true) },
            failure = { return Result.Success(false) },
        )
    }

    private suspend fun requestCreateAccount(): Result<String> {
        return accountCreator.createAccount(blockchain = blockchain, walletPublicKey = wallet.publicKey.blockchainKey)
    }

    private fun TransactionData.setTransactionHash(transactionToSend: TransferTransaction) {
        // Uses java.time.Instant which is available from 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val validStart = transactionToSend.transactionId.validStart
            hash = buildString {
                append(wallet.address)
                append(HEDERA_EXPLORER_LINK_DELIMITER)
                append(validStart?.epochSecond.toString())
                append(HEDERA_EXPLORER_LINK_DELIMITER)
                append(validStart?.nano.toString())
            }
        }
    }

    private companion object {
        // https://docs.hedera.com/hedera/networks/mainnet/fees
        val HBAR_TRANSFER_USD_COST = BigDecimal("0.0001")
        val HBAR_CREATE_ACCOUNT_USD_COST = BigDecimal("0.05")
        // Hedera fees are low, allow 10% safety margin to allow usage of not precise fee estimate
        val MAX_FEE_MULTIPLIER = BigDecimal("1.1")
        const val HEDERA_EXPLORER_LINK_DELIMITER = "-"
    }
}