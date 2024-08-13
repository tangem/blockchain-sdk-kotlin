package com.tangem.blockchain.blockchains.tron.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.BigInteger

class TronNetworkService(
    rpcNetworkProviders: List<TronNetworkProvider>,
    private val blockchain: Blockchain,
) {

    private val multiProvider = MultiNetworkProvider(rpcNetworkProviders)
    val host: String
        get() = multiProvider.currentProvider.baseUrl

    suspend fun getAccountInfo(
        address: String,
        tokens: Collection<Token>,
        transactionIds: List<String>,
    ): Result<TronAccountInfo> {
        return coroutineScope {
            val tokenBalancesDeferred = tokens.map { async { getTokenBalance(address, it) } }
            val confirmedTransactionsDeferred =
                transactionIds.map { async { checkIfTransactionConfirmed(it) } }
            when (
                val accountInfoResult = multiProvider.performRequest(TronNetworkProvider::getAccount, address)
            ) {
                is Result.Failure -> Result.Failure(accountInfoResult.error)
                is Result.Success -> {
                    Result.Success(
                        TronAccountInfo(
                            balance = BigDecimal(accountInfoResult.data.balance ?: 0)
                                .movePointLeft(blockchain.decimals()),
                            tokenBalances = tokenBalancesDeferred.awaitAll()
                                .mapNotNull { if (it is Result.Success) it.data else null }.toMap(),
                            confirmedTransactionIds = confirmedTransactionsDeferred.awaitAll()
                                .mapNotNull { if (it is Result.Success) it.data else null },
                        ),
                    )
                }
            }
        }
    }

    suspend fun getAllowance(ownerAddress: String, token: Token, spenderAddress: String): kotlin.Result<BigDecimal> {
        val allowanceRequest = multiProvider.performRequest(
            TronNetworkProvider::getAllowance,
            TokenAllowanceRequestData(ownerAddress, token.contractAddress, spenderAddress),
        )
        return when (allowanceRequest) {
            is Result.Failure -> kotlin.Result.failure(allowanceRequest.error)
            is Result.Success -> {
                val allowance = allowanceRequest.data.constantResult.firstOrNull()
                    ?: return kotlin.Result.failure(BlockchainSdkError.CustomError("Failed to get allowance"))
                kotlin.Result.success(allowance.hexToBigDecimal())
            }
        }
    }

    suspend fun getNowBlock(): Result<TronBlock> {
        return multiProvider.performRequest(TronNetworkProvider::getNowBlock)
    }

    suspend fun broadcastHex(data: ByteArray): Result<TronBroadcastResponse> {
        return when (val result = multiProvider.performRequest(TronNetworkProvider::broadcastHex, data)) {
            is Result.Failure -> Result.Failure(result.error)
            is Result.Success -> {
                if (result.data.result) {
                    Result.Success(result.data)
                } else {
                    Result.Failure(
                        BlockchainSdkError.CustomError(
                            result.data.errorMessage ?: "error sending transaction",
                        ),
                    )
                }
            }
        }
    }

    suspend fun getMaxEnergyUse(address: String, contractAddress: String, parameter: String): Result<Int> {
        val result =
            multiProvider.performRequest { contractEnergyUsage(address, contractAddress, parameter) }
        return when (result) {
            is Result.Failure -> Result.Failure(result.error)
            is Result.Success -> {
                val maxEnergy = result.data.energyUsed
                Result.Success(maxEnergy)
            }
        }
    }

    suspend fun getAccountResource(address: String): Result<TronGetAccountResourceResponse> {
        return multiProvider.performRequest(TronNetworkProvider::getAccountResource, address)
    }

    suspend fun checkIfAccountExists(address: String): Boolean {
        return when (val result = getAccount(address)) {
            is Result.Failure -> false
            is Result.Success -> result.data.address != null
        }
    }

    suspend fun getAccount(address: String): Result<TronGetAccountResponse> {
        return multiProvider.performRequest(TronNetworkProvider::getAccount, address)
    }

    suspend fun getChainParameters(): Result<TronChainParameters> {
        return when (val result = multiProvider.performRequest(TronNetworkProvider::getChainParameters)) {
            is Result.Failure -> Result.Failure(result.error)
            is Result.Success -> {
                val energyFee = result.data.chainParameters
                    .firstOrNull { it.key == KEY_SUN_ENERGY_FEE }
                    ?.value
                val energyMaxFactor = result.data.chainParameters
                    .firstOrNull { it.key == KEY_MAX_FACTOR }
                    ?.value
                val increaseFactor = result.data.chainParameters
                    .firstOrNull { it.key == KEY_INCREASE_FACTOR }
                    ?.value

                if (energyFee != null && energyMaxFactor != null && increaseFactor != null) {
                    Result.Success(
                        TronChainParameters(
                            sunPerEnergyUnit = energyFee,
                            dynamicEnergyMaxFactor = energyMaxFactor,
                            dynamicIncreaseFactor = increaseFactor,
                        ),
                    )
                } else {
                    Result.Failure(BlockchainSdkError.CustomError("Can not get necessary chain parameters"))
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun getTokenBalance(address: String, token: Token): Result<Pair<Token, BigDecimal>> {
        val result = multiProvider.performRequest(
            TronNetworkProvider::getTokenBalance,
            TokenBalanceRequestData(address, token.contractAddress),
        )
        when (result) {
            is Result.Failure -> {
                return Result.Failure(result.error)
            }

            is Result.Success -> {
                val hexValue = result.data.constantResult.firstOrNull()?.ifBlank { "0" }
                    ?: return Result.Failure(BlockchainSdkError.CustomError("FailedToParseNetworkResponse"))

                // Take the first 32 bytes of the hexadecimal string to correctly display the token balance. ([REDACTED_TASK_KEY])
                val hexValue32 = hexValue.take(64)
                val value = BigInteger(hexValue32, 16).toBigDecimal(token.decimals)
                return Result.Success(token to value)
            }
        }
    }

    private suspend fun checkIfTransactionConfirmed(id: String): Result<String?> {
        return multiProvider.performRequest(TronNetworkProvider::getTransactionInfoById, id)
    }
}

private const val KEY_SUN_ENERGY_FEE = "getEnergyFee"
private const val KEY_MAX_FACTOR = "getDynamicEnergyMaxFactor"
private const val KEY_INCREASE_FACTOR = "getDynamicEnergyIncreaseFactor"