package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairEthNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairToken
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal


class EthereumNetworkService(
    jsonRpcProviders: List<EthereumJsonRpcProvider>,
    private val blockcypherNetworkProvider: BlockcypherNetworkProvider? = null,
    private val blockchairEthNetworkProvider: BlockchairEthNetworkProvider? = null,
) : EthereumNetworkProvider {

    private val multiJsonRpcProvider = MultiNetworkProvider(jsonRpcProviders)
    override val host
        get() = multiJsonRpcProvider.currentProvider.host

    private val decimals = Blockchain.Ethereum.decimals()

    override suspend fun getInfo(
        address: String,
        tokens: Set<Token>,
    ): Result<EthereumInfoResponse> {
        return try {
            coroutineScope {
                val balanceResponseDeferred = async {
                    multiJsonRpcProvider
                        .performRequest(EthereumJsonRpcProvider::getBalance, address)
                }
                val txCountResponseDeferred = async {
                    multiJsonRpcProvider
                        .performRequest(EthereumJsonRpcProvider::getTxCount, address)
                }
                val pendingTxCountResponseDeferred = async {
                    multiJsonRpcProvider
                        .performRequest(EthereumJsonRpcProvider::getPendingTxCount, address)
                }
                val transactionsResponseDeferred = async {
                    blockchairEthNetworkProvider?.getTransactions(address, tokens)
                }

                val balance = balanceResponseDeferred.await().extractResult()
                    .parseAmount(decimals)
                val txCount = txCountResponseDeferred.await().extractResult()
                    .responseToBigInteger().toLong()
                val pendingTxCount = pendingTxCountResponseDeferred.await().extractResult()
                    .responseToBigInteger().toLong()

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
            Result.Failure(error)
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            multiJsonRpcProvider
                .performRequest(EthereumJsonRpcProvider::sendTransaction, transaction)
                .extractResult()
            SimpleResult.Success
        } catch (error: Exception) {
            SimpleResult.Failure(error)
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
            Result.Failure(error)
        }
    }

    private suspend fun getTokensBalanceInternal(
        address: String,
        tokens: Set<Token>,
    ): Map<Token, BigDecimal> {
        return coroutineScope {
            val tokenBalancesDeferred = tokens.map { token ->
                token to async {
                    multiJsonRpcProvider.performRequest(
                        EthereumJsonRpcProvider::getTokenBalance,
                        EthereumTokenBalanceRequestData(
                            address,
                            token.contractAddress
                        )
                    )
                }
            }.toMap()
            val tokenBalanceResponses = tokenBalancesDeferred.mapValues { it.value.await() }
            tokenBalanceResponses.mapValues {
                it.value.extractResult().parseAmount(it.key.decimals)
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
                val gasPriceResponses = multiJsonRpcProvider.providers.map {
                    async { it.getGasPrice() }
                }.map { it.await() }

                val gasPrice = gasPriceResponses.filter { it is Result.Success }
                    .map { it.extractResult().responseToLong() }.maxOrNull()
                // all responses have failed
                    ?: return@coroutineScope Result.Failure(
                        (gasPriceResponses.first() as Result.Failure).error
                    )

                Result.Success(gasPrice)
            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }

    }

    override suspend fun getGasLimit(to: String, from: String, data: String?): Result<Long> {
        return try {
            val gasLimit = multiJsonRpcProvider.performRequest(
                EthereumJsonRpcProvider::getGasLimit,
                EthCallObject(to, from, data)
            ).extractResult().responseToLong()
            Result.Success(gasLimit)
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    private fun String.responseToBigInteger() =
        this.substring(2).toBigInteger(16)

    private fun String.responseToLong() = this.responseToBigInteger().toLong()

    private fun String.parseAmount(decimals: Int) =
        this.responseToBigInteger().toBigDecimal().movePointLeft(decimals)

    private fun EthereumError.toException() =
        Exception("Code: ${this.code}, ${this.message}")

    private fun Result<EthereumResponse>.extractResult(): String =
        when (this) {
            is Result.Success -> this.data.result
                ?: throw this.data.error?.toException()
                    ?: Exception("Unknown response format")
            is Result.Failure -> throw this.error
                ?: Exception()
        }
}