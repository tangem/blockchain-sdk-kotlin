package com.tangem.blockchain.blockchains.cosmos.network

import com.tangem.blockchain.blockchains.cosmos.CosmosAccountInfo
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

class CosmosNetworkService(
    providers: List<CosmosRestProvider>,
    private val cosmosChain: CosmosChain,
) {

    private val multiJsonRpcProvider = MultiNetworkProvider(providers)

    val host: String get() = multiJsonRpcProvider.currentProvider.host

    suspend fun getAccountInfo(address: String): Result<CosmosAccountInfo> {
        return coroutineScope {
            val accountResult = multiJsonRpcProvider.performRequest(CosmosRestProvider::accounts, address)
            val balancesResult = multiJsonRpcProvider.performRequest(CosmosRestProvider::balances, address)

            val accounts = when (accountResult) {
                is Result.Failure -> return@coroutineScope Result.Failure(BlockchainSdkError.AccountNotFound)
                is Result.Success -> accountResult.data
            }
            val balances = when (balancesResult) {
                is Result.Failure -> return@coroutineScope Result.Failure(BlockchainSdkError.AccountNotFound)
                is Result.Success -> balancesResult.data
            }

            val accountNumber = accounts?.account?.accountNumber?.toLongOrNull()
            val sequenceNumber = accounts?.account?.sequence?.toLongOrNull() ?: 0L
            val amount = parseBalance(balances)

            Result.Success(
                CosmosAccountInfo(
                    accountNumber = accountNumber,
                    sequenceNumber = sequenceNumber,
                    amount = amount,
                )
            )
        }
    }

    suspend fun estimateGas(requestBody: String): Result<Long> {
        val simulateResult = multiJsonRpcProvider.performRequest(CosmosRestProvider::simulate, requestBody)
        val simulateResponse = simulateResult.successOr { return it }
        return Result.Success(simulateResponse.gasInfo.gasUsed)
    }

    suspend fun send(requestBody: String): Result<String> {
        return try {
            val txResult = multiJsonRpcProvider.performRequest(CosmosRestProvider::txs, requestBody)
            val txInfo = txResult.successOr { return it }.txInfo
            val height = txInfo.height.toLongOrNull() ?: 0
            if (height <= 0) Result.Failure(BlockchainSdkError.FailedToSendException) else Result.Success(txInfo.txhash)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun parseBalance(balanceResponse: CosmosBalanceResponse): Amount {
        val blockchain = cosmosChain.blockchain
        val balanceAmountString = balanceResponse.balances
            .firstOrNull { it.denom == cosmosChain.smallestDenomination }?.amount
            ?: return Amount(blockchain = blockchain)

        val balanceAmount = balanceAmountString.toLongOrNull() ?: throw BlockchainSdkError.AccountNotFound
        return Amount(
            blockchain = blockchain,
            value = BigDecimal.valueOf(balanceAmount).movePointLeft(blockchain.decimals()),
        )
    }
}