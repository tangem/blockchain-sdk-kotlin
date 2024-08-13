package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.toBlockchainSdkError
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

open class EthereumNetworkService(
    jsonRpcProviders: List<EthereumJsonRpcProvider>,
    private val blockcypherNetworkProvider: BlockcypherNetworkProvider? = null,
    private val blockchairEthNetworkProvider: BlockchairEthNetworkProvider? = null,
) : EthereumNetworkProvider {

    private val multiJsonRpcProvider = MultiNetworkProvider(jsonRpcProviders)
    override val baseUrl
        get() = multiJsonRpcProvider.currentProvider.baseUrl

    private val decimals = Blockchain.Ethereum.decimals()

    override suspend fun getInfo(address: String, tokens: Set<Token>): Result<EthereumInfoResponse> {
        return try {
            coroutineScope {
                val balanceResponseDeferred = async {
                    multiJsonRpcProvider.performRequest(EthereumJsonRpcProvider::getBalance, address)
                }
                val txCountResponseDeferred = async {
                    multiJsonRpcProvider.performRequest(EthereumJsonRpcProvider::getTxCount, address)
                }
                val pendingTxCountResponseDeferred = async {
                    multiJsonRpcProvider.performRequest(EthereumJsonRpcProvider::getPendingTxCount, address)
                }
                val transactionsResponseDeferred = async {
                    blockchairEthNetworkProvider?.getTransactions(address, tokens)
                }

                val balance = balanceResponseDeferred.await().extractResult().let { balanceResponse ->
                    requireNotNull(
                        value = EthereumUtils.parseEthereumDecimal(value = balanceResponse, decimalsCount = decimals),
                        lazyMessage = { "Error while parsing balance. Balance response: $balanceResponse" },
                    )
                }
                val txCount = txCountResponseDeferred.await().extractResult().responseToBigInteger().toLong()
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
                        recentTransactions = recentTransactions,
                    ),
                )
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getPendingTxCount(address: String): Result<Long> {
        return try {
            val response = multiJsonRpcProvider.performRequest(EthereumJsonRpcProvider::getPendingTxCount, address)
            Result.Success(
                response
                    .extractResult()
                    .responseToBigInteger()
                    .toLong(),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getAllowance(
        ownerAddress: String,
        token: Token,
        spenderAddress: String,
    ): kotlin.Result<BigDecimal> {
        return try {
            val requestData = EthereumTokenAllowanceRequestData(ownerAddress, token.contractAddress, spenderAddress)
            val amountValue = multiJsonRpcProvider.performRequest(
                request = EthereumJsonRpcProvider::getTokenAllowance,
                data = requestData,
            ).extractResult().parseAmount(token.decimals)

            kotlin.Result.success(amountValue)
        } catch (exception: Exception) {
            kotlin.Result.failure(exception.toBlockchainSdkError())
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
            val txId = multiJsonRpcProvider
                .performRequest(EthereumJsonRpcProvider::sendTransaction, transaction)
                .extractResult()
            Result.Success(txId)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return blockcypherNetworkProvider?.getSignatureCount(address)
            ?: Result.Failure(BlockchainSdkError.CustomError("No signature count provider found"))
    }

    override suspend fun getTokensBalance(address: String, tokens: Set<Token>): Result<Map<Token, BigDecimal>> {
        return try {
            Result.Success(getTokensBalanceInternal(address, tokens))
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private suspend fun getTokensBalanceInternal(address: String, tokens: Set<Token>): Map<Token, BigDecimal> {
        return coroutineScope {
            val tokenBalancesDeferred = tokens.map { token ->
                token to async {
                    multiJsonRpcProvider.performRequest(
                        EthereumJsonRpcProvider::getTokenBalance,
                        EthereumTokenBalanceRequestData(
                            address,
                            token.contractAddress,
                        ),
                    )
                }
            }.toMap()
            val tokenBalanceResponses = tokenBalancesDeferred.mapValues { it.value.await() }
            tokenBalanceResponses.mapValues {
                requireNotNull(EthereumUtils.parseEthereumDecimal(it.value.extractResult(), it.key.decimals)) {
                    "Failed to parse token balance. Token: ${it.key.name}. Balance: ${it.value.extractResult()}"
                }
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
                val gasPrice = multiJsonRpcProvider.performRequest(
                    EthereumJsonRpcProvider::getGasPrice,
                ).extractResult().responseToBigInteger()

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
                    EthCallObject(to, from, value, data),
                ).extractResult().responseToBigInteger()
                Result.Success(gasLimit)
            }
        } catch (exception: Exception) {
            if (exception.message?.contains("gas required exceeds allowance", true) == true) {
                Result.Failure(
                    Exception("Not enough funds for the transaction. Please top up your account.")
                        .toBlockchainSdkError(),
                )
            } else {
                Result.Failure(exception.toBlockchainSdkError())
            }
        }
    }

    override suspend fun callContractForFee(data: ContractCallData): Result<BigInteger> {
        return try {
            val result = multiJsonRpcProvider.performRequest(EthereumJsonRpcProvider::call, data)
                .extractResult().responseToBigInteger()
            Result.Success(result)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    @Suppress("MagicNumber")
    private fun String.responseToBigInteger() = this.substring(2).ifBlank { "0" }.toBigInteger(16)

    private fun String.parseAmount(decimals: Int) = this.responseToBigInteger().toBigDecimal().movePointLeft(decimals)

    private fun Result<EthereumResponse>.extractResult(): String = when (this) {
        is Result.Success -> {
            this.data.result
                ?: throw this.data.error?.let { error ->
                    BlockchainSdkError.Ethereum.Api(
                        code = error.code ?: 0,
                        message = error.message ?: "No error message",
                    )
                } ?: BlockchainSdkError.CustomError("Unknown response format")
        }
        is Result.Failure -> {
            throw this.error as? BlockchainSdkError ?: BlockchainSdkError.CustomError("Unknown error format")
        }
    }
}