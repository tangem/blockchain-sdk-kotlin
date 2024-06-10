package com.tangem.blockchain.blockchains.polkadot.network

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.toHexString
import io.emeraldpay.polkaj.api.PolkadotMethod.CHAIN_GET_BLOCK_HASH
import io.emeraldpay.polkaj.api.PolkadotMethod.CHAIN_GET_HEADER
import org.kethereum.extensions.hexToBigInteger
import org.komputing.khex.model.HexString
import java.math.BigDecimal
import java.math.BigInteger

internal class PolkadotJsonRpcProvider(
    baseUrl: String,
    credentials: Map<String, String>?,
) {

    val host: String = baseUrl

    private val api = createRetrofitInstance(
        baseUrl = baseUrl,
        headerInterceptors = if (credentials != null) listOf(AddHeaderInterceptor(credentials)) else emptyList(),
    ).create(PolkadotApi::class.java)

    @Throws
    suspend fun getFee(transaction: ByteArray, decimals: Int): BigDecimal {
        val response = RpcBody(
            PolkadotMethod.GET_FEE.method,
            listOf("0x" + transaction.toHexString()),
        ).post()
        return response.extractResult().getFee(decimals)
    }

    @Throws
    suspend fun getLatestBlockHash(): Result<String> {
        val latestBlockHash = RpcBody(
            CHAIN_GET_BLOCK_HASH,
        ).postWithStringResult().successOr { return it }.result
            ?: return Result.Failure(BlockchainSdkError.CustomError("hash is null"))
        return Result.Success(latestBlockHash)
    }

    suspend fun getBlockNumber(blockhash: String): Result<BigInteger> {
        val blockNumber = RpcBody(
            CHAIN_GET_HEADER,
            params = listOf(blockhash),
        ).post().extractResult()["number"] as? String
            ?: return Result.Failure(BlockchainSdkError.CustomError("wrong block number"))
        return Result.Success(HexString(blockNumber).hexToBigInteger())
    }

    private suspend fun RpcBody.post(): Result<RpcMapResponse> {
        return try {
            val result = retryIO { api.post(this) }
            Result.Success(result)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private suspend fun RpcBody.postWithStringResult(): Result<RpcStringResponse> {
        return try {
            val result = retryIO { api.postWithStringResult(this) }
            Result.Success(result)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private fun Result<RpcMapResponse>.extractResult(): Map<String, Any> = when (this) {
        is Result.Success -> {
            this.data.result
                ?: throw this.data.error?.let { error ->
                    BlockchainSdkError.Polkadot.ApiWithCode(
                        code = error.code ?: 0,
                        message = error.message ?: "No error message",
                    )
                } ?: BlockchainSdkError.CustomError("Unknown response format")
        }
        is Result.Failure -> {
            throw this.error as? BlockchainSdkError ?: BlockchainSdkError.CustomError("Unknown error format")
        }
    }

    private fun Map<String, Any>.getFee(decimals: Int): BigDecimal {
        val feeString = this[FEE] as? String
        return feeString?.toBigDecimal()?.movePointLeft(decimals) ?: BigDecimal.ZERO
    }

    companion object {
        const val FEE = "partialFee"
    }
}