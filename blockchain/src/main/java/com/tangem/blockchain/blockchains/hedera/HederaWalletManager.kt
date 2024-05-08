package com.tangem.blockchain.blockchains.hedera

import android.util.Log
import com.hedera.hashgraph.sdk.*
import com.tangem.blockchain.blockchains.hedera.models.HederaAccountInfo
import com.tangem.blockchain.blockchains.hedera.models.HederaTransactionId
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkService
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
    private val networkService: HederaNetworkService,
    private val dataStorage: AdvancedDataStorage,
    private val accountCreator: AccountCreator,
) : WalletManager(wallet), TransactionSender {

    private val blockchain = wallet.blockchain

    override val currentHost: String
        get() = networkService.baseUrl

    override suspend fun updateInternal() {
        when (val getAccountIdResult = getAccountId()) {
            is Result.Success -> updateAccountInfo(getAccountIdResult.data)
            is Result.Failure -> updateError(getAccountIdResult.error)
        }
    }

    private suspend fun updateAccountInfo(accountId: String) {
        val pendingTxs = wallet.recentTransactions
            .filter { it.status == TransactionStatus.Unconfirmed }
            .mapNotNullTo(hashSetOf()) { it.hash?.let { hash -> HederaTransactionId.fromRawStringId(hash) } }
        when (val balances = networkService.getAccountInfo(accountId, pendingTxs)) {
            is Result.Success -> updateWallet(balances.data)
            is Result.Failure -> updateError(balances.error)
        }
    }

    private fun updateWallet(accountInfo: HederaAccountInfo) {
        Log.d(this::class.java.simpleName, "Balance is ${accountInfo.balance.hbarBalance}")

        wallet.changeAmountValue(AmountType.Coin, accountInfo.balance.hbarBalance.movePointLeft(blockchain.decimals()))
        cardTokens.forEach { token ->
            val tokenBalance = accountInfo.balance.tokenBalances
                .find { token.contractAddress == it.contractAddress }
                ?.balance
                ?.movePointLeft(token.decimals)
                ?: BigDecimal.ZERO
            wallet.addTokenValue(tokenBalance, token)
        }
        accountInfo.pendingTxsInfo.forEach { txInfo ->
            wallet.recentTransactions.find { it.hash == txInfo.id.rawStringId }?.let { txData ->
                txData.status = if (txInfo.isPending) TransactionStatus.Unconfirmed else TransactionStatus.Confirmed
            }
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) error("Error isn't BlockchainSdkError")
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        return transactionBuilder.buildToSign(transactionData)
            .map { buildTransaction ->
                val sendResult = sendTransaction(signer, buildTransaction)
                if (sendResult is Result.Success) {
                    transactionData.setTransactionHash(sendResult.data.transactionId)
                    wallet.addOutgoingTransaction(transactionData)
                }
            }.toSimpleResult()
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return when (val usdExchangeRateResult = networkService.getUsdExchangeRate()) {
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

    private suspend fun sendTransaction(
        signer: TransactionSigner,
        builtTransaction: HederaBuiltTransaction,
    ): Result<TransactionResponse> {
        return try {
            when (val signerResult = signer.sign(builtTransaction.signatures, wallet.publicKey)) {
                is CompletionResult.Success -> {
                    val transactionToSend = transactionBuilder.buildToSend(
                        transaction = builtTransaction.transferTransaction,
                        signatures = signerResult.data,
                    )
                    networkService.sendTransaction(transactionToSend)
                }
                is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
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
                networkService.getAccountId(wallet.publicKey.blockchainKey.toCompressedPublicKey())
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
        return networkService.getAccountId(alias.toBytesRaw()).fold(
            success = { return Result.Success(true) },
            failure = { return Result.Success(false) },
        )
    }

    private suspend fun requestCreateAccount(): Result<String> {
        return accountCreator.createAccount(blockchain = blockchain, walletPublicKey = wallet.publicKey.blockchainKey)
    }

    private fun TransactionData.setTransactionHash(transactionId: TransactionId) {
        hash = HederaTransactionId.fromTransactionId(transactionId).rawStringId
    }

    private companion object {
        // https://docs.hedera.com/hedera/networks/mainnet/fees
        val HBAR_TRANSFER_USD_COST = BigDecimal("0.0001")
        val HBAR_CREATE_ACCOUNT_USD_COST = BigDecimal("0.05")
        // Hedera fees are low, allow 10% safety margin to allow usage of not precise fee estimate
        val MAX_FEE_MULTIPLIER = BigDecimal("1.1")
    }
}