package com.tangem.blockchain.blockchains.vechain.network

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TokenBalanceERC20TokenMethod
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenMethod
import com.tangem.blockchain.blockchains.vechain.VeChainAccountInfo
import com.tangem.blockchain.blockchains.vechain.VeChainBlockInfo
import com.tangem.blockchain.blockchains.vechain.VeChainWalletManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.common.extensions.mapNotNullValues
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.BigInteger

// Sync2 uses `20_000_000` as a maximum allowed gas amount for such contract calls.
private const val MAX_ALLOWED_VM_GAS = 20_000_000
// Placeholder value, not used in contract calls.
private const val CONTRACT_CALL_VALUE = "0"

internal class VeChainNetworkService(
    networkProviders: List<VeChainNetworkProvider>,
    private val blockchain: Blockchain,
) {

    private val multiJsonRpcProvider = MultiNetworkProvider(networkProviders)
    val host: String get() = multiJsonRpcProvider.currentProvider.baseUrl

    suspend fun getAccountInfo(
        address: String,
        pendingTxIds: Set<String>,
        tokens: Set<Token>,
    ): Result<VeChainAccountInfo> {
        return try {
            coroutineScope {
                val accountInfoDeferred = async {
                    multiJsonRpcProvider.performRequest(VeChainNetworkProvider::getAccountInfo, address)
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

    suspend fun getLatestBlock(): Result<VeChainBlockInfo> {
        return multiJsonRpcProvider.performRequest(VeChainNetworkProvider::getLatestBlock)
    }

    suspend fun sendTransaction(rawData: ByteArray): Result<VeChainCommitTransactionResponse> {
        return multiJsonRpcProvider.performRequest {
            sendTransaction(rawData)
        }
    }

    suspend fun getVmGas(source: String, destination: String, amount: Amount, token: Token): Result<Long> {
        val amountValue = amount.value?.movePointRight(amount.decimals)?.toBigInteger() ?: BigInteger.ZERO
        val clause = VeChainClause(
            to = token.contractAddress,
            value = CONTRACT_CALL_VALUE,
            data = "0x" + TransferERC20TokenMethod(destination = destination, amount = amountValue).data.toHexString(),
        )
        val request = VeChainContractCallRequest(
            clauses = listOf(clause),
            caller = source,
            gas = MAX_ALLOWED_VM_GAS,
        )
        return multiJsonRpcProvider.performRequest { this.callContract(request = request) }.map {
            val response = it.firstOrNull() ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)
            if (!response.vmError.isNullOrEmpty()) return Result.Failure(BlockchainSdkError.FailedToLoadFee)
            response.gasUsed ?: 0
        }
    }

    private suspend fun getTokenBalances(address: String, tokens: Set<Token>): Map<Token, BigDecimal> {
        return coroutineScope {
            val tokenBalancesDeferred = tokens.associateWith { token ->
                async {
                    multiJsonRpcProvider.performRequest {
                        val clause = VeChainClause(
                            to = token.contractAddress,
                            value = CONTRACT_CALL_VALUE,
                            data = "0x" + TokenBalanceERC20TokenMethod(address = address).data.toHexString(),
                        )
                        this.callContract(
                            request = VeChainContractCallRequest(
                                clauses = listOf(clause),
                                caller = null,
                                gas = null,
                            ),
                        )
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
        accountResponse: VeChainGetAccountResponse,
        pendingTxsInfo: List<VeChainTransactionInfoResponse?>,
        tokenBalances: Map<Token, BigDecimal>,
    ): VeChainAccountInfo {
        val balance = accountResponse.balance.hexToBigDecimal().movePointLeft(blockchain.decimals())
        val energy = accountResponse.energy.hexToBigDecimal().movePointLeft(VeChainWalletManager.VTHO_TOKEN.decimals)
        return VeChainAccountInfo(
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
