package com.tangem.blockchain.blockchains.hedera.network

import com.hedera.hashgraph.sdk.*
import com.tangem.blockchain.blockchains.hedera.models.HederaAccountBalance
import com.tangem.blockchain.blockchains.hedera.models.HederaAccountInfo
import com.tangem.blockchain.blockchains.hedera.models.HederaTransactionId
import com.tangem.blockchain.blockchains.hedera.models.HederaTransactionInfo
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.*
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

internal class HederaNetworkService(private val client: Client, hederaNetworkProviders: List<HederaNetworkProvider>) {

    private val multiProvider = MultiNetworkProvider(hederaNetworkProviders)

    val baseUrl: String get() = multiProvider.currentProvider.baseUrl

    suspend fun getAccountId(publicKey: ByteArray): Result<String> {
        return multiProvider.performRequest(HederaNetworkProvider::getAccountId, publicKey)
    }

    suspend fun getUsdExchangeRate(): Result<BigDecimal> {
        return multiProvider.performRequest(HederaNetworkProvider::getUsdExchangeRate)
    }

    suspend fun getAccountInfo(accountId: String, pendingTxIds: Set<HederaTransactionId>): Result<HederaAccountInfo> {
        return try {
            coroutineScope {
                val balancesDeferred = async { getBalance(accountId) }
                val txsDeferred = pendingTxIds.map { async { getTransactionInfo(it) } }

                constructHederaAccountInfo(
                    balanceResult = balancesDeferred.await(),
                    txsResult = txsDeferred.awaitAll(),
                )
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    fun <T : Transaction<T>> sendTransaction(transaction: Transaction<T>): Result<TransactionResponse> {
        return try {
            Result.Success(transaction.execute(client))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun constructHederaAccountInfo(
        balanceResult: Result<HederaAccountBalance>,
        txsResult: List<Result<HederaTransactionInfo>>,
    ): Result<HederaAccountInfo> {
        val balance = balanceResult.successOr { return it }
        val txs = txsResult.map { infoResult -> infoResult.successOr { return it } }
        return Result.Success(HederaAccountInfo(balance = balance, pendingTxsInfo = txs))
    }

    private suspend fun getBalance(accountId: String): Result<HederaAccountBalance> {
        return multiProvider.performRequest(HederaNetworkProvider::getBalances, accountId)
            .map { it.balances.firstOrNull()?.toDomain() }
            // In case mirror node respond with failure or with empty balance we will redirect call to consensus node
            .fold(
                success = { response ->
                    if (response != null) Result.Success(response) else getBalanceFromConsensusNode(accountId)
                },
                failure = { getBalanceFromConsensusNode(accountId) },
            )
    }

    private suspend fun getTransactionInfo(transactionId: HederaTransactionId): Result<HederaTransactionInfo> {
        return multiProvider.performRequest(HederaNetworkProvider::getTransactionInfo, transactionId.rawStringId)
            .fold(
                success = { tx ->
                    Result.Success(
                        HederaTransactionInfo(
                            isPending = tx.result == "OK",
                            id = HederaTransactionId.fromRawStringId(tx.transactionId),
                        ),
                    )
                },
                failure = { getTransactionInfoFromConsensusNode(transactionId) },
            )
    }

    private fun getTransactionInfoFromConsensusNode(transactionId: HederaTransactionId): Result<HederaTransactionInfo> {
        return try {
            val transaction = TransactionReceiptQuery()
                .setTransactionId(transactionId.transactionId)
                .execute(client)
            Result.Success(
                HederaTransactionInfo(
                    isPending = transaction.status == Status.OK,
                    id = HederaTransactionId.fromTransactionId(requireNotNull(transaction.transactionId)),
                ),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun getBalanceFromConsensusNode(accountId: String): Result<HederaAccountBalance> {
        return try {
            val balance = AccountBalanceQuery()
                .setAccountId(AccountId.fromString(accountId))
                .execute(client)
            Result.Success(balance.toDomain())
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun HederaBalanceResponse.toDomain(): HederaAccountBalance = HederaAccountBalance(
        hbarBalance = balance.toBigDecimal(),
        tokenBalances = tokenBalances.map { tokenBalanceResponse ->
            HederaAccountBalance.TokenBalance(
                contractAddress = tokenBalanceResponse.tokenId,
                balance = tokenBalanceResponse.balance.toBigDecimal(),
            )
        },
    )

    private fun AccountBalance.toDomain(): HederaAccountBalance = HederaAccountBalance(
        hbarBalance = this.hbars.toTinybars().toBigDecimal(),
        tokenBalances = this.tokens.map { (tokenId, balance) ->
            HederaAccountBalance.TokenBalance(
                contractAddress = tokenId.toString(),
                balance = balance.toBigDecimal(),
            )
        },
    )
}