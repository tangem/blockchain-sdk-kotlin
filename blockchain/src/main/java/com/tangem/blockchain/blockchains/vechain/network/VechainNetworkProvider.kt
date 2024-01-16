package com.tangem.blockchain.blockchains.vechain.network

import com.tangem.blockchain.blockchains.vechain.VechainBlockInfo
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import org.komputing.khex.extensions.toHexString

internal class VechainNetworkProvider(
    override val baseUrl: String,
    val api: VechainApi,
) : NetworkProvider {

    suspend fun getAccountInfo(address: String): Result<VechainGetAccountResponse> {
        return try {
            val response = api.getAccount(address)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    /**
     * Returns null when [includePending] is false, but transaction with [txId] in pending status
     */
    suspend fun getTransactionInfo(txId: String, includePending: Boolean): Result<VechainTransactionInfoResponse?> {
        return try {
            val response = api.getTransactionInfo(transactionId = txId, pending = includePending)
            Result.Success(response.body())
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun getTokenBalance(request: VechainTokenBalanceRequest): Result<List<VechainData>> {
        return try {
            val response = api.getTokenBalance(requestBody = request)
            Result.Success(response)
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

    private fun mapBlockInfo(response: VechainLatestBlockResponse): VechainBlockInfo {
        val blockRef = response.blockId
            .removePrefix("0x")
            .substring(range = 0..BLOCK_REFERENCE_LENGTH)
            .toLongOrNull(radix = 16)
        return VechainBlockInfo(
            blockId = response.blockId,
            blockRef = blockRef ?: 0,
            blockNumber = response.number,
        )
    }

    private companion object {

        const val BLOCK_REFERENCE_LENGTH = 15
    }
}