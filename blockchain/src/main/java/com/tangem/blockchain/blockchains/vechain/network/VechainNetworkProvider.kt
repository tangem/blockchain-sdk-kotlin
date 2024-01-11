package com.tangem.blockchain.blockchains.vechain.network

import com.tangem.blockchain.blockchains.vechain.VechainAccountInfo
import com.tangem.blockchain.blockchains.vechain.VechainBlockInfo
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.hexToBigDecimal
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.komputing.khex.extensions.toHexString
import retrofit2.Response

private const val VETHOR_DECIMALS = 18

internal class VechainNetworkProvider(
    override val baseUrl: String,
    val api: VechainApi,
) : NetworkProvider {

    suspend fun getAccountInfo(decimals: Int, address: String, pendingTxIds: Set<String>): Result<VechainAccountInfo> {
        return try {
            coroutineScope {
                val accountInfo = async { api.getAccount(address) }
                val pendingTxsInfo =
                    pendingTxIds.map { txId -> async { api.getTransactionInfo(transactionId = txId, pending = false) } }
                Result.Success(
                    mapAccountInfo(
                        decimals = decimals,
                        accountResponse = accountInfo.await(),
                        pendingTxsInfo = pendingTxsInfo.awaitAll(),
                    )
                )
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun getLatestBlock(): Result<VechainBlockInfo> {
        return try {
            val response = api.getLatestBlockInfo()
            Result.Success(mapBlockInfo(response))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun sendTransaction(rawData: ByteArray): Result<VechainCommitTransactionResponse> {
        return try {
            val body = VechainCommitTransactionRequest(rawData.toHexString())
            val response = api.commitTransaction(body)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun mapAccountInfo(
        decimals: Int,
        accountResponse: VechainGetAccountResponse,
        pendingTxsInfo: List<Response<VechainTransactionInfoResponse?>>,
    ): VechainAccountInfo {
        val balance = accountResponse.balance.hexToBigDecimal().movePointLeft(decimals)
        val energy = accountResponse.energy.hexToBigDecimal().movePointLeft(VETHOR_DECIMALS)
        return VechainAccountInfo(
            balance = balance,
            energy = energy,
            completedTxIds = pendingTxsInfo.mapNotNullTo(hashSetOf()) { it.body()?.txId })
    }

    private fun mapBlockInfo(response: VechainLatestBlockResponse): VechainBlockInfo {
        val blockRef = response.blockId
            .removePrefix("0x")
            .substring(0..15)
            .toLongOrNull(radix = 16)
        return VechainBlockInfo(
            blockId = response.blockId,
            blockRef = blockRef ?: 0,
            blockNumber = response.number,
        )
    }
}