package com.tangem.blockchain.blockchains.vechain.network

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TokenBalanceERC20TokenMethod
import com.tangem.blockchain.blockchains.vechain.VechainAccountInfo
import com.tangem.blockchain.blockchains.vechain.VechainBlockInfo
import com.tangem.blockchain.blockchains.vechain.VechainWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.common.extensions.mapNotNullValues
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

internal class VechainNetworkService(
    networkProviders: List<VechainNetworkProvider>,
    private val blockchain: Blockchain,
) {

    private val multiJsonRpcProvider = MultiNetworkProvider(networkProviders)
    val host: String get() = multiJsonRpcProvider.currentProvider.baseUrl

    suspend fun getAccountInfo(
        address: String,
        pendingTxIds: Set<String>,
        tokens: Set<Token>,
    ): Result<VechainAccountInfo> {
        return try {
            coroutineScope {
                val accountInfoDeferred = async {
                    multiJsonRpcProvider.performRequest(VechainNetworkProvider::getAccountInfo, address)
                }
                val pendingTxsDeferred = pendingTxIds.map { txId ->
                    async {
                        multiJsonRpcProvider.performRequest { getTransactionInfo(txId = txId, includePending = false) }
                    }
                }
                val tokenBalances = getTokenBalances(address, tokens)
                Result.Success(
                    mapAccountInfo(
                        accountResponse = accountInfoDeferred.await().extractResult(),
                        pendingTxsInfo = pendingTxsDeferred.awaitAll().map { it.extractResult() },
                        tokenBalances = tokenBalances,
                    ),
                )
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun getLatestBlock(): Result<VechainBlockInfo> {
        return multiJsonRpcProvider.performRequest(VechainNetworkProvider::getLatestBlock)
    }

    suspend fun sendTransaction(rawData: ByteArray): Result<VechainCommitTransactionResponse> {
        return multiJsonRpcProvider.performRequest {
            sendTransaction(rawData)
        }
    }

    private suspend fun getTokenBalances(address: String, tokens: Set<Token>): Map<Token, BigDecimal> {
        return coroutineScope {
            val tokenBalancesDeferred = tokens.associateWith { token ->
                async {
                    multiJsonRpcProvider.performRequest {
                        val clause = VechainClause(
                            to = token.contractAddress,
                            value = "0",
                            data = "0x" + TokenBalanceERC20TokenMethod(address = address).data.toHexString(),
                        )
                        this.getTokenBalance(request = VechainTokenBalanceRequest(clauses = listOf(clause)))
                    }
                }
            }
            val tokenBalanceResponses = tokenBalancesDeferred.mapValues { it.value.await() }
            tokenBalanceResponses.mapNotNullValues {
                val response = it.value.extractResult()
                response.firstOrNull()?.data?.hexToBigDecimal()?.movePointLeft(it.key.decimals)
            }
        }
    }

    private fun mapAccountInfo(
        accountResponse: VechainGetAccountResponse,
        pendingTxsInfo: List<VechainTransactionInfoResponse?>,
        tokenBalances: Map<Token, BigDecimal>,
    ): VechainAccountInfo {
        val balance = accountResponse.balance.hexToBigDecimal().movePointLeft(blockchain.decimals())
        val energy = accountResponse.energy.hexToBigDecimal().movePointLeft(VechainWalletManager.VTHO_TOKEN.decimals)
        return VechainAccountInfo(
            balance = balance,
            energy = energy,
            completedTxIds = pendingTxsInfo.mapNotNullTo(hashSetOf()) { it?.txId },
            tokenBalances = tokenBalances,
        )
    }

    private fun <T> Result<T>.extractResult(): T = when (this) {
        is Result.Success -> this.data
        is Result.Failure -> {
            throw this.error as? BlockchainSdkError ?: BlockchainSdkError.CustomError("Unknown error format")
        }
    }
}