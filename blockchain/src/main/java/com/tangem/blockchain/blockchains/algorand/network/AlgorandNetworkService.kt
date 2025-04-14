package com.tangem.blockchain.blockchains.algorand.network

import com.tangem.blockchain.blockchains.algorand.models.AlgorandAccountModel
import com.tangem.blockchain.blockchains.algorand.models.AlgorandEstimatedFeeParams
import com.tangem.blockchain.blockchains.algorand.models.AlgorandTransactionBuildParams
import com.tangem.blockchain.blockchains.algorand.models.AlgorandTransactionInfo
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.common.extensions.guard
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import kotlin.math.max

/**
 * This parameter descripe transaction is valid if submitted between rounds.
 * Look at this [doc](https://developer.algorand.org/docs/get-details/transactions/).
 */
private const val BOUNCE_ROUND_VALUE = 1000L

internal class AlgorandNetworkService(
    networkProviders: List<AlgorandNetworkProvider>,
    private val blockchain: Blockchain,
) {

    val host: String get() = multiJsonRpcProvider.currentProvider.baseUrl

    private val multiJsonRpcProvider = MultiNetworkProvider(networkProviders)

    suspend fun getAccountInfo(address: String, pendingTxHashes: Set<String>): Result<AlgorandAccountModel> {
        return try {
            coroutineScope {
                val balanceDeferred = async { getAccountBalance(address) }
                val txsDeferred = pendingTxHashes.map { async { getPendingTransaction(it) } }

                Result.Success(
                    constructAlgorandAccountModel(
                        balanceResult = balanceDeferred.await(),
                        txsResult = txsDeferred.awaitAll(),
                    ),
                )
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun getEstimatedFee(): Result<AlgorandEstimatedFeeParams> {
        return multiJsonRpcProvider
            .performRequest(AlgorandNetworkProvider::getTransactionParams)
            .map { response ->
                val sourceFee = response.fee.toBigDecimal().correctAlgorandDecimals()
                val minFee = response.minFee.toBigDecimal().correctAlgorandDecimals()

                AlgorandEstimatedFeeParams(minFee = minFee, fee = sourceFee)
            }
    }

    suspend fun getTransactionParams(): Result<AlgorandTransactionBuildParams> {
        return multiJsonRpcProvider
            .performRequest(AlgorandNetworkProvider::getTransactionParams)
            .map { response ->
                AlgorandTransactionBuildParams(
                    genesisId = response.genesisId,
                    genesisHash = response.genesisHash,
                    firstRound = response.lastRound,
                    lastRound = response.lastRound + BOUNCE_ROUND_VALUE,
                )
            }
    }

    suspend fun sendTransaction(rawData: ByteArray): Result<String> {
        return multiJsonRpcProvider
            .performRequest(AlgorandNetworkProvider::sendTransaction, rawData)
            .map(AlgorandTransactionResultResponse::txId)
    }

    private suspend fun getAccountBalance(address: String): Result<AlgorandBalance> {
        return multiJsonRpcProvider
            .performRequest(AlgorandNetworkProvider::getAccount, address)
            .map(::calculateCoinValueWithReserveDeposit)
    }

    private suspend fun getPendingTransaction(txHash: String): Result<AlgorandTransactionInfo?> {
        return multiJsonRpcProvider
            .performRequest(AlgorandNetworkProvider::getPendingTransaction, txHash)
            .map { response ->
                if (response?.confirmedRound != null) {
                    val confirmedRound = response.confirmedRound.guard {
                        return Result.Success(null)
                    }
                    when {
                        confirmedRound > 0 -> AlgorandTransactionInfo(
                            transactionHash = txHash,
                            status = AlgorandTransactionInfo.Status.COMMITTED,
                        )
                        confirmedRound == 0L && response.poolError.isEmpty() -> AlgorandTransactionInfo(
                            transactionHash = txHash,
                            status = AlgorandTransactionInfo.Status.STILL,
                        )
                        confirmedRound == 0L && response.poolError.isNotEmpty() -> AlgorandTransactionInfo(
                            transactionHash = txHash,
                            status = AlgorandTransactionInfo.Status.REMOVED,
                        )
                        else -> throw BlockchainSdkError.CustomError("Unknown response format")
                    }
                } else {
                    null
                }
            }
    }

    private fun calculateCoinValueWithReserveDeposit(accountResponse: AlgorandAccountResponse): AlgorandBalance {
        val changeBalanceValue = max(accountResponse.amount - accountResponse.minBalance, 0)
        val availableCoinBalance = changeBalanceValue.toBigDecimal().correctAlgorandDecimals()

        val reserveCoinBalance = accountResponse.minBalance.toBigDecimal().correctAlgorandDecimals()
        return AlgorandBalance(
            availableCoinBalance = availableCoinBalance,
            reserveBalance = reserveCoinBalance,
            balanceIncludingReserve = accountResponse.amount.toBigDecimal().correctAlgorandDecimals(),
        )
    }

    private fun constructAlgorandAccountModel(
        balanceResult: Result<AlgorandBalance>,
        txsResult: List<Result<AlgorandTransactionInfo?>>,
    ): AlgorandAccountModel {
        val balance = balanceResult.successOr { throw it.error }
        val txs = txsResult.mapNotNull { it.successOr { null } }

        return AlgorandAccountModel(
            availableCoinBalance = balance.availableCoinBalance,
            reserveValue = balance.reserveBalance,
            balanceIncludingReserve = balance.balanceIncludingReserve,
            transactionsInfo = txs,
        )
    }

    private fun BigDecimal.correctAlgorandDecimals() = this.movePointLeft(blockchain.decimals())

    private data class AlgorandBalance(
        val availableCoinBalance: BigDecimal,
        val reserveBalance: BigDecimal,
        val balanceIncludingReserve: BigDecimal,
    )
}