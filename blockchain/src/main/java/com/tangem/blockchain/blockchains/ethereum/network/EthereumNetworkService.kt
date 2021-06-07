package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairEthNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairToken
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal


class EthereumNetworkService(
    private val jsonRpcProviders: List<EthereumJsonRpcProvider>,
    private val blockcypherNetworkProvider: BlockcypherNetworkProvider? = null,
    private val blockchairEthNetworkProvider: BlockchairEthNetworkProvider? = null,
) : MultiNetworkProvider<EthereumJsonRpcProvider>(jsonRpcProviders), EthereumNetworkProvider {

    private val jsonRpcProvider
        get() = currentProvider
    private val decimals = Blockchain.Ethereum.decimals()

    override suspend fun getInfo(
        address: String,
        tokens: Set<Token>,
    ): Result<EthereumInfoResponse> {
        return try {
            coroutineScope {
                val balanceResponseDeferred =
                    retryIO { async { jsonRpcProvider.getBalance(address) } }
                val txCountResponseDeferred =
                    retryIO { async { jsonRpcProvider.getTxCount(address) } }
                val pendingTxCountResponseDeferred =
                    retryIO { async { jsonRpcProvider.getPendingTxCount(address) } }
                val transactionsResponseDeferred =
                    retryIO {
                        async {
                            blockchairEthNetworkProvider?.getTransactions(address,
                                tokens)
                        }
                    }


                val balanceResponse = balanceResponseDeferred.await()
                val balance = balanceResponse.result?.parseAmount(decimals)
                    ?: throw balanceResponse.error?.toException()
                        ?: Exception("Unknown balance response format")

                val txCount = txCountResponseDeferred.await()
                    .result?.responseToBigInteger()?.toLong() ?: 0
                val pendingTxCount = pendingTxCountResponseDeferred.await()
                    .result?.responseToBigInteger()?.toLong() ?: 0

                val tokenBalances = getTokensBalanceInternal(address, tokens)

                val recentTransactions = when (val result = transactionsResponseDeferred.await()) {
                    is Result.Success -> result.data
                    else -> emptyList()
                }

                Result.Success(
                    EthereumInfoResponse(
                        coinBalance = balance,
                        tokenBalances = tokenBalances,
                        txCount = txCount,
                        pendingTxCount = pendingTxCount,
                        recentTransactions = recentTransactions
                    )
                )
            }
        } catch (error: Exception) {
            val result = Result.Failure(error)
            return if (result.needsRetry()) getInfo(address, tokens) else result
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val response = retryIO { jsonRpcProvider.sendTransaction(transaction) }
            if (response.error == null) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(response.error.toException())
            }
        } catch (error: Exception) {
            val result = SimpleResult.Failure(error)
            return if (result.needsRetry()) sendTransaction(transaction) else result
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return blockcypherNetworkProvider?.getSignatureCount(address)
            ?: Result.Failure(Exception("No signature count provider found"))
    }

    override suspend fun getTokensBalance(
        address: String,
        tokens: Set<Token>,
    ): Result<Map<Token, BigDecimal>> {
        return try {
            Result.Success(getTokensBalanceInternal(address, tokens))
        } catch (error: Exception) {
            val result = Result.Failure(error)
            return if (result.needsRetry()) getTokensBalance(address, tokens) else result
        }
    }

    private suspend fun getTokensBalanceInternal(
        address: String,
        tokens: Set<Token>,
    ): Map<Token, BigDecimal> {
        return coroutineScope {
            val tokenBalancesDeferred = tokens.map { token ->
                token to retryIO {
                    async {
                        jsonRpcProvider.getTokenBalance(address,
                            token.contractAddress)
                    }
                }
            }.toMap()
            val tokenBalanceResponses = tokenBalancesDeferred.mapValues { it.value.await() }
            tokenBalanceResponses.mapValues {
                it.value.result?.parseAmount(it.key.decimals)
                    ?: throw it.value.error?.toException()
                        ?: Exception("Unknown token balance response format")
            }
        }
    }

    override suspend fun findErc20Tokens(address: String): Result<List<BlockchairToken>> {
        return blockchairEthNetworkProvider?.findErc20Tokens(address)
            ?: Result.Failure(Exception("Unsupported feature"))
    }

    override suspend fun getGasPrice(): Result<Long> {
        return try {
            coroutineScope {
                val gasPriceResponses = jsonRpcProviders.map {
                    async { retryIO { jsonRpcProvider.getGasPrice() } }
                }.map { it.await() }

                val gasPrice = gasPriceResponses
                    .mapNotNull { it.result?.responseToLong() }.maxOrNull()
                    ?: throw gasPriceResponses.mapNotNull { it.error }
                        .firstOrNull()?.toException()
                        ?: Exception("Unknown gas price response format")
                Result.Success(gasPrice)
            }
        } catch (exception: Exception) {
            val result = Result.Failure(exception)
            if (result.needsRetry()) getGasPrice() else result
        }

    }

    override suspend fun getGasLimit(
        to: String,
        from: String,
        data: String?,
        fallbackGasLimit: Long?,
    ): Result<Long> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred = jsonRpcProviders.map {
                    async { retryIO { jsonRpcProvider.getGasLimit(to, from, data) } }
                }
                val gasLimitResponses = gasLimitResponsesDeferred.map { it.await() }
                val gasLimit = gasLimitResponses
                    .mapNotNull { it.result?.responseToLong() }.maxOrNull()
                    ?: fallbackGasLimit
                    ?: throw gasLimitResponses.mapNotNull { it.error }
                        .firstOrNull()?.toException()
                        ?: Exception("Unknown estimate gas response format")
                Result.Success(gasLimit)
            }
        } catch (error: Exception) {
            val result = Result.Failure(error)
            return if (result.needsRetry()) getGasLimit(to,
                from,
                data,
                fallbackGasLimit) else result
        }
    }

    private fun String.responseToBigInteger() =
        this.substring(2).toBigInteger(16)

    private fun String.responseToLong() = this.responseToBigInteger().toLong()

    private fun String.parseAmount(decimals: Int) =
        this.responseToBigInteger().toBigDecimal().movePointLeft(decimals)

    private fun EthereumError.toException() =
        Exception("Code: ${this.code}, ${this.message}")

}