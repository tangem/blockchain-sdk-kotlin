package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairEthNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairToken
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.BigInteger

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
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getAllowance(ownerAddress: String, token: Token, spenderAddress: String): Result<Amount> {
        return try {
            Result.Success(
                Amount(
                    token, multiJsonRpcProvider.performRequest(
                    EthereumJsonRpcProvider::getTokenAllowance,
                    EthereumTokenAllowanceRequestData(
                        ownerAddress,
                        token.contractAddress,
                        spenderAddress
                    )
                ).extractResult().parseAmount(token.decimals)
                )
            )
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            multiJsonRpcProvider
                .performRequest(EthereumJsonRpcProvider::sendTransaction, transaction)
                .extractResult()
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }

    suspend fun sendRawTransaction(transaction: String): Result<String> {
        return try {
            val tx_id = multiJsonRpcProvider
                .performRequest(EthereumJsonRpcProvider::sendTransaction, transaction)
                .extractResult()
            Result.Success(tx_id)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return blockcypherNetworkProvider?.getSignatureCount(address)
            ?: Result.Failure(BlockchainSdkError.CustomError("No signature count provider found"))
    }

    override suspend fun getTokensBalance(
        address: String,
        tokens: Set<Token>,
    ): Result<Map<Token, BigDecimal>> {
        return try {
            Result.Success(getTokensBalanceInternal(address, tokens))
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
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
            ?: Result.Failure(BlockchainSdkError.CustomError("Unsupported feature"))
    }

    override suspend fun getGasPrice(): Result<BigInteger> {
        return try {
            coroutineScope {
                val gasPriceResponses = multiJsonRpcProvider.providers.map {
                    async { it.getGasPrice() }
                }.map { it.await() }

                val gasPrice = gasPriceResponses.filter { it is Result.Success }
                    .map { it.extractResult().responseToBigInteger() }.maxOrNull()
                // all responses have failed
                    ?: return@coroutineScope Result.Failure(
                        (gasPriceResponses.first() as Result.Failure).error
                    )

                Result.Success(gasPrice)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }

    }

    override suspend fun getGasLimit(to: String, from: String, value: String?, data: String?): Result<BigInteger> {
        return try {
            coroutineScope {
                val gasLimit = multiJsonRpcProvider.performRequest(
                    EthereumJsonRpcProvider::getGasLimit,
                    EthCallObject(to, from, value, data)
                ).extractResult().responseToBigInteger()
                Result.Success(gasLimit)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private fun String.responseToBigInteger() =
        this.substring(2).ifBlank { "0" }.toBigInteger(16)

    private fun String.parseAmount(decimals: Int) =
        this.responseToBigInteger().toBigDecimal().movePointLeft(decimals)

    private fun EthereumError.toException() =
        Exception("Code: ${this.code}, ${this.message}")

    private fun Result<EthereumResponse>.extractResult(): String =
        when (this) {
            is Result.Success -> {
                this.data.result
                    ?: throw this.data.error?.toException()?.toBlockchainSdkError()
                        ?: BlockchainSdkError.CustomError("Unknown response format")
            }

            is Result.Failure -> {
                throw (this.error as? BlockchainSdkError)
                    ?: BlockchainSdkError.CustomError("Unknown error format")
            }
        }
}