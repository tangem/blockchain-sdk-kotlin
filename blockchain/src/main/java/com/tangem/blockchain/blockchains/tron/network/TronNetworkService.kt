package com.tangem.blockchain.blockchains.tron.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.BigInteger

class TronNetworkService(
    private val rpcNetworkProvider: TronJsonRpcNetworkProvider,
    private val blockchain: Blockchain
) {

    suspend fun getAccountInfo(
        address: String, tokens: Collection<Token>, transactionIds: List<String>
    ): Result<TronAccountInfo> {
        return coroutineScope {
            val tokenBalancesDeferred = tokens.map { async { getTokenBalance(address, it) } }
            val confirmedTransactionsDeferred =
                transactionIds.map { async { checkIfTransactionConfirmed(it) } }

            when (val accountInfoResult = rpcNetworkProvider.getAccount(address)) {
                is Result.Failure -> Result.Failure(accountInfoResult.error)
                is Result.Success -> {
                    Result.Success(TronAccountInfo(
                        balance = BigDecimal(accountInfoResult.data.balance ?: 0)
                            .movePointLeft(blockchain.decimals()),
                        tokenBalances = tokenBalancesDeferred.awaitAll()
                            .mapNotNull { if (it is Result.Success) it.data else null }.toMap(),
                        confirmedTransactionIds = confirmedTransactionsDeferred.awaitAll()
                            .mapNotNull { if (it is Result.Success) it.data else null }
                    )
                    )
                }
            }
        }
    }

    suspend fun getNowBlock(): Result<TronBlock> {
        return rpcNetworkProvider.getNowBlock()
    }

    suspend fun broadcastHex(data: ByteArray): Result<TronBroadcastResponse> {
        return when (val result = rpcNetworkProvider.broadcastHex(data)) {
            is Result.Failure -> Result.Failure(result.error)
            is Result.Success -> {
                if (result.data.result) {
                    Result.Success(result.data)
                } else {
                    Result.Failure(BlockchainSdkError.CustomError(
                        result.data.errorMessage ?: "error sending transaction")
                    )
                }
            }
        }
    }

    suspend fun getMaxEnergyUse(contractAddress: String?): Result<Int> {
        if (contractAddress == null) return Result.Success(0) // for non-token transactions
        val result = rpcNetworkProvider.getTokenTransactionHistory(contractAddress)
        return when (result) {
            is Result.Failure -> Result.Failure(result.error)
            is Result.Success -> {
                val maxEnergy = result.data.data.mapNotNull { it.energyUsageTotal }.maxOrNull() ?: 0
                Result.Success(maxEnergy)
            }
        }
    }

    suspend fun getAccountResource(address: String): Result<TronGetAccountResourceResponse> {
        return rpcNetworkProvider.getAccountResource(address)
    }

    suspend fun checkIfAccountExists(address: String): Boolean {
        return when (val result = getAccount(address)) {
            is Result.Failure -> false
            is Result.Success -> result.data.address != null
        }
    }

    suspend fun getAccount(address: String): Result<TronGetAccountResponse> {
        return rpcNetworkProvider.getAccount(address)
    }

    suspend fun getChainParameters(): Result<TronChainParameters> {
        return when (val result = rpcNetworkProvider.getChainParameters()) {
            is Result.Failure -> Result.Failure(result.error)
            is Result.Success -> {
                val energyFee = result.data.chainParameters
                    .firstOrNull { it.key == KEY_SUN_ENERGY_FEE }
                    ?.value
                val energyMaxFactor = result.data.chainParameters
                    .firstOrNull { it.key == KEY_MAX_FACTOR }
                    ?.value

                if (energyFee != null && energyMaxFactor != null) {
                    Result.Success(
                        TronChainParameters(
                            sunPerEnergyUnit = energyFee,
                            dynamicEnergyMaxFactor = energyMaxFactor,
                        )
                    )
                } else {
                    Result.Failure(BlockchainSdkError.CustomError("Can not get necessary chain parameters"))
                }
            }
        }
    }

    private suspend fun getTokenBalance(
        address: String,
        token: Token,
    ): Result<Pair<Token, BigDecimal>> {
        val result = rpcNetworkProvider.run { getTokenBalance(address, token.contractAddress) }
        when (result) {
            is Result.Failure -> {
                return Result.Failure(result.error)
            }
            is Result.Success -> {
                val hexValue = result.data.constantResult.firstOrNull()?.ifBlank { "0" }
                    ?: return Result.Failure(BlockchainSdkError.CustomError("FailedToParseNetworkResponse"))
                val value = BigInteger(hexValue, 16).toBigDecimal(token.decimals)
                return Result.Success(token to value)
            }
        }

    }

    private suspend fun checkIfTransactionConfirmed(id: String): Result<String?> {
        return rpcNetworkProvider.getTransactionInfoById(id)
    }
}

private const val KEY_SUN_ENERGY_FEE = "getEnergyFee"
private const val KEY_MAX_FACTOR = "getDynamicEnergyMaxFactor"
