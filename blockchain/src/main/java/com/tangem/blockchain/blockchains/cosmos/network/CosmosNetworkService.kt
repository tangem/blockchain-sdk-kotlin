package com.tangem.blockchain.blockchains.cosmos.network

import com.tangem.blockchain.blockchains.cosmos.CosmosAccountInfo
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

class CosmosNetworkService(
    providers: List<CosmosRestProvider>,
    private val cosmosChain: CosmosChain,
) {

    private val multiJsonRpcProvider = MultiNetworkProvider(providers)

    val host: String get() = multiJsonRpcProvider.currentProvider.baseUrl

    suspend fun getAccountInfo(
        address: String,
        tokens: Set<Token>,
        unconfirmedTxsHashes: List<String>,
    ): Result<CosmosAccountInfo> {
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
            val confirmedTxHashes = getConfirmedTxsHashes(unconfirmedTxsHashes)

            val accountNumber = accounts?.account?.accountNumber
            val sequenceNumber = accounts?.account?.sequence ?: 0L
            val amount = parseBalance(
                balances,
                cosmosChain.smallestDenomination,
                cosmosChain.blockchain.decimals(),
            )
            val tokenAmounts = tokens.mapNotNull { token ->
                val denomination =
                    cosmosChain.tokenDenominationByContractAddress[token.contractAddress] ?: return@mapNotNull null
                val balance = parseBalance(balances, denomination, token.decimals)
                token to balance
            }.toMap()

            Result.Success(
                CosmosAccountInfo(
                    accountNumber = accountNumber,
                    sequenceNumber = sequenceNumber,
                    amount = amount,
                    tokenBalances = tokenAmounts,
                    confirmedTransactionHashes = confirmedTxHashes,
                ),
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

            if (txInfo.code == 0) {
                Result.Success(txInfo.txhash)
            } else {
                Result.Failure(BlockchainSdkError.FailedToSendException)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private suspend fun getConfirmedTxsHashes(unconfirmedTxsHashes: List<String>): List<String> {
        return coroutineScope {
            unconfirmedTxsHashes.map { hash ->
                async { multiJsonRpcProvider.performRequest(CosmosRestProvider::checkTransactionStatus, hash) }
            }
        }
            .awaitAll()
            // Ignore failed requests
            .mapNotNull { (it as? Result.Success<CosmosTxResponse>)?.data?.txInfo?.txhash }
    }

    private fun parseBalance(balanceResponse: CosmosBalanceResponse, denomination: String, decimals: Int): Amount {
        val balanceAmount = balanceResponse.balances.firstOrNull { it.denom == denomination }?.amount
            ?: return Amount(blockchain = cosmosChain.blockchain)

        return Amount(
            blockchain = cosmosChain.blockchain,
            value = BigDecimal.valueOf(balanceAmount).movePointLeft(decimals),
        )
    }
}
