package com.tangem.blockchain.blockchains.vechain.network

import com.tangem.blockchain.blockchains.vechain.VeChainBlockInfo
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import org.komputing.khex.extensions.toHexString

internal class VeChainNetworkProvider(
    override val baseUrl: String,
    val api: VeChainApi,
) : NetworkProvider {

    suspend fun getAccountInfo(address: String): Result<VeChainGetAccountResponse> {
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
    suspend fun getTransactionInfo(txId: String, includePending: Boolean): Result<VeChainTransactionInfoResponse?> {
        return try {
            val response = api.getTransactionInfo(transactionId = txId, pending = includePending)
            Result.Success(response.body())
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun callContract(request: VeChainContractCallRequest): Result<List<VeChainContractCallResponse>> {
        return try {
            val response = api.callContract(requestBody = request)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun getLatestBlock(): Result<VeChainBlockInfo> {
        return try {
            val response = api.getLatestBlockInfo()
            Result.Success(mapBlockInfo(response))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun sendTransaction(rawData: ByteArray): Result<VeChainCommitTransactionResponse> {
        return try {
            val body = VeChainCommitTransactionRequest(rawData.toHexString())
            val response = api.commitTransaction(body)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun mapBlockInfo(response: VeChainLatestBlockResponse): VeChainBlockInfo {
        val blockRef = response.blockId
            .removePrefix("0x")
            .substring(range = 0..BLOCK_REFERENCE_LENGTH)
            .toLongOrNull(radix = 16)
        return VeChainBlockInfo(
            blockId = response.blockId,
            blockRef = blockRef ?: 0,
            blockNumber = response.number,
        )
    }

    private companion object {

        const val BLOCK_REFERENCE_LENGTH = 15
    }
}
