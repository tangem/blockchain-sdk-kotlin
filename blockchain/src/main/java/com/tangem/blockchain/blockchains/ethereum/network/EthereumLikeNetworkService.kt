package com.tangem.blockchain.blockchains.ethereum.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.converters.ENSResponseConverter
import com.tangem.blockchain.blockchains.ethereum.converters.ENSReverseResponseConverter
import com.tangem.blockchain.blockchains.ethereum.converters.EthereumFeeHistoryConverter
import com.tangem.blockchain.blockchains.ethereum.models.EthereumFeeHistoryResponse
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairEthNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairToken
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.blockchain.network.moshi
import com.tangem.blockchain.yieldsupply.DefaultYieldSupplyProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Abstract base class for Ethereum-like network services
 * Supports different RPC method prefixes (eth_, quai_, etc.)
 */
@OptIn(ExperimentalStdlibApi::class)
internal abstract class EthereumLikeNetworkService(
    private val multiJsonRpcProvider: MultiNetworkProvider<out EthereumLikeJsonRpcProvider>,
    private val yieldSupplyProvider: YieldSupplyProvider = DefaultYieldSupplyProvider,
    private val blockcypherNetworkProvider: BlockcypherNetworkProvider? = null,
    private val blockchairEthNetworkProvider: BlockchairEthNetworkProvider? = null,
) : EthereumNetworkProvider {

    override val baseUrl get() = multiJsonRpcProvider.currentProvider.baseUrl
    private val stringAdapter by lazy { moshi.adapter<String>() }
    private val feeHistoryAdapter by lazy { moshi.adapter<EthereumFeeHistoryResponse>() }

    /**
     * Get the decimals for this blockchain
     */
    protected abstract fun getDecimals(): Int

    override suspend fun getInfo(address: String, tokens: Set<Token>): Result<EthereumInfoResponse> {
        return try {
            coroutineScope {
                val balanceResponseDeferred = async {
                    multiJsonRpcProvider.performRequest(EthereumLikeJsonRpcProvider::getBalance, address)
                }
                val txCountResponseDeferred = async {
                    multiJsonRpcProvider.performRequest(EthereumLikeJsonRpcProvider::getTxCount, address)
                }
                val pendingTxCountResponseDeferred = async {
                    multiJsonRpcProvider.performRequest(EthereumLikeJsonRpcProvider::getPendingTxCount, address)
                }
                val transactionsResponseDeferred = async {
                    blockchairEthNetworkProvider?.getTransactions(address, tokens)
                }

                val balance = balanceResponseDeferred.await().extractResult().let { balanceResponse ->
                    requireNotNull(
                        value = EthereumUtils.parseEthereumDecimal(
                            value = balanceResponse,
                            decimalsCount = getDecimals(),
                        ),
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
            val response = multiJsonRpcProvider.performRequest(EthereumLikeJsonRpcProvider::getPendingTxCount, address)
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

    override suspend fun getTxCount(address: String): Result<BigInteger> {
        return try {
            val response = multiJsonRpcProvider.performRequest(EthereumLikeJsonRpcProvider::getPendingTxCount, address)
            Result.Success(
                response
                    .extractResult()
                    .responseToBigInteger(),
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
                request = EthereumLikeJsonRpcProvider::getTokenAllowance,
                data = requestData,
            ).extractResult().parseAmount(token.decimals)

            kotlin.Result.success(amountValue)
        } catch (exception: Exception) {
            kotlin.Result.failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: String): Result<String> {
        return try {
            val response = multiJsonRpcProvider
                .performRequest(EthereumLikeJsonRpcProvider::sendTransaction, transaction)
                .extractResult()
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return blockcypherNetworkProvider?.getSignatureCount(address)
            ?: Result.Failure(BlockchainSdkError.CustomError("No signature count provider found"))
    }

    override suspend fun getTokensBalance(address: String, tokens: Set<Token>): Result<List<Amount>> {
        return try {
            Result.Success(getTokensBalanceInternal(address, tokens))
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private suspend fun getTokensBalanceInternal(address: String, tokens: Set<Token>): List<Amount> {
        return coroutineScope {
            val isYieldSupported = yieldSupplyProvider.isSupported()

            tokens.map { token ->
                async {
                    if (isYieldSupported && yieldSupplyProvider.getYieldModuleAddress() != EthereumUtils.ZERO_ADDRESS) {
                        val yieldLendingStatus = yieldSupplyProvider.getYieldSupplyStatus(token.contractAddress)
                        if (yieldLendingStatus?.isActive == true) {
                            yieldSupplyProvider.getBalance(yieldLendingStatus, token)
                        } else {
                            getTokenBalance(address, token)
                        }
                    } else {
                        getTokenBalance(address, token)
                    }
                }
            }.awaitAll()
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
                    EthereumLikeJsonRpcProvider::getGasPrice,
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
                    EthereumLikeJsonRpcProvider::getGasLimit,
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

    override suspend fun getFeeHistory(): Result<EthereumFeeHistory> {
        return try {
            val response = multiJsonRpcProvider.performRequest(EthereumLikeJsonRpcProvider::getFeeHistory)
                .extractResult(feeHistoryAdapter)

            val feeHistory = runCatching { EthereumFeeHistoryConverter.convert(response) }
                .getOrElse {
                    val gasPrice = getGasPrice().successOr { return it }

                    EthereumFeeHistory.Fallback(gasPrice)
                }

            Result.Success(feeHistory)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun callContractForFee(data: ContractCallData): Result<BigInteger> {
        return try {
            val result = multiJsonRpcProvider.performRequest(EthereumLikeJsonRpcProvider::call, data)
                .extractResult().responseToBigInteger()
            Result.Success(result)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun resolveName(namehash: ByteArray, encodedName: ByteArray): ResolveAddressResult {
        return try {
            val data = EthereumResolveENSNameRequestData(
                contractAddress = RESOLVE_ENS_NAME_CONTRACT_ADDRESS,
                nameBytes = encodedName,
                callDataBytes = READ_ETHEREUM_ADDRESS_INTERFACE_ID + namehash,
            )

            val resultString = multiJsonRpcProvider.performRequest(EthereumLikeJsonRpcProvider::resolveENSName, data)
                .extractResult()

            val result = ENSResponseConverter.convert(resultString)

            ResolveAddressResult.Resolved(result)
        } catch (exception: Exception) {
            ResolveAddressResult.Error(exception.toBlockchainSdkError())
        }
    }

    override suspend fun resolveAddress(address: String): ReverseResolveAddressResult {
        return try {
            val data = EthereumReverseResolveENSAddressRequestData(
                address = address.hexToBytes(),
                contractAddress = RESOLVE_ENS_NAME_CONTRACT_ADDRESS,
            )

            val resultString = multiJsonRpcProvider.performRequest(
                request = EthereumLikeJsonRpcProvider::reverseResolveENSAddress,
                data = data,
            )
                .extractResult()

            val result = ENSReverseResponseConverter.convert(resultString)

            ReverseResolveAddressResult.Resolved(result)
        } catch (exception: Exception) {
            ReverseResolveAddressResult.Error(exception.toBlockchainSdkError())
        }
    }

    private suspend fun getTokenBalance(address: String, token: Token): Amount {
        val rawTokenBalance = multiJsonRpcProvider.performRequest(
            EthereumLikeJsonRpcProvider::getTokenBalance,
            EthereumTokenBalanceRequestData(
                address,
                token.contractAddress,
            ),
        )

        val tokenBalance = requireNotNull(
            EthereumUtils.parseEthereumDecimal(
                rawTokenBalance.extractResult(),
                token.decimals,
            ),
        ) { "Failed to parse token balance. Token: ${token.name}. Balance: ${rawTokenBalance.extractResult()}" }

        return Amount(
            token = token,
            value = tokenBalance,
        )
    }

    override suspend fun getContractNonce(address: String): Result<BigInteger> {
        return try {
            // Standard nonce() function signature
            val functionSignature = "0xaffed0e0" // keccak256("nonce()")[:4]

            val data = functionSignature

            val callData = ContractCallData(
                to = address,
                data = data,
            )

            val result = multiJsonRpcProvider.performRequest(EthereumLikeJsonRpcProvider::call, callData)
                .extractResult().responseToBigInteger()

            Result.Success(result)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    @Suppress("MagicNumber")
    private fun String.responseToBigInteger() = this.substring(2).ifBlank { "0" }.toBigInteger(16)

    private fun String.parseAmount(decimals: Int) = this.responseToBigInteger().toBigDecimal().movePointLeft(decimals)

    private fun Result<JsonRPCResponse>.extractResult(): String = extractResult(adapter = stringAdapter)

    private fun <Body> Result<JsonRPCResponse>.extractResult(adapter: JsonAdapter<Body>): Body {
        return when (this) {
            is Result.Success -> {
                runCatching { adapter.fromJsonValue(data.result) }.getOrNull()
                    ?: throw data.error?.let { error ->
                        BlockchainSdkError.Ethereum.getApiErrorByCode(error.code, error.message)
                    } ?: BlockchainSdkError.CustomError("Unknown response format")
            }
            is Result.Failure -> {
                throw error as? BlockchainSdkError ?: BlockchainSdkError.CustomError("Unknown error format")
            }
        }
    }

    private companion object {
        private const val RESOLVE_ENS_NAME_CONTRACT_ADDRESS = "0x64969fb44091A7E5fA1213D30D7A7e8488edf693"
        private val READ_ETHEREUM_ADDRESS_INTERFACE_ID = "0x3b3b57de".hexToBytes()
    }
}