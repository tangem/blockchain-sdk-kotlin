package com.tangem.blockchain.blockchains.polkadot.network

import com.tangem.blockchain.blockchains.polkadot.models.PolkadotRuntimeDispatchInfo
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.bytes4LittleEndian
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.remove
import com.tangem.common.extensions.toHexString
import io.emeraldpay.polkaj.api.PolkadotMethod.*
import io.emeraldpay.polkaj.scale.ScaleCodecReader
import org.kethereum.extensions.hexToBigInteger
import org.komputing.khex.model.HexString
import org.ton.tl.ByteString.Companion.decodeFromHex
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
        val response = createRpcBody(
            method = STATE_CALL,
            params = listOf(
                PolkadotMethod.GET_FEE.method,
                HEX_PREFIX + transaction.toHexString() + transaction.size.bytes4LittleEndian().toHexString(),
            ),
        ).post()

        return when (response) {
            is Result.Success -> parseFee(resultHex = response.data.result.toString(), decimals = decimals)
            is Result.Failure -> throw response.error
        }
    }

    @Throws
    suspend fun getLatestBlockHash(): Result<String> {
        val latestBlockHash = createRpcBody(method = CHAIN_GET_BLOCK_HASH)
            .post()
            .successOr { return it }
            .result as? String
            ?: return Result.Failure(BlockchainSdkError.CustomError("hash is null"))

        return Result.Success(latestBlockHash)
    }

    suspend fun getBlockNumber(blockhash: String): Result<BigInteger> {
        val blockNumber = createRpcBody(method = CHAIN_GET_HEADER, params = listOf(blockhash))
            .post()
            .extractResult()["number"] as? String
            ?: return Result.Failure(BlockchainSdkError.CustomError("wrong block number"))

        return Result.Success(HexString(blockNumber).hexToBigInteger())
    }

    private fun createRpcBody(method: String, params: List<Any> = emptyList()): JsonRPCRequest {
        return JsonRPCRequest(method = method, params = params, id = "4")
    }

    private suspend fun JsonRPCRequest.post(): Result<JsonRPCResponse> {
        return try {
            val result = retryIO { api.post(this) }
            Result.Success(result)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private fun Result<JsonRPCResponse>.extractResult(): Map<String, Any> = when (this) {
        is Result.Success -> {
            data.result as? Map<String, Any>
                ?: throw data.error?.let { error ->
                    BlockchainSdkError.Polkadot.ApiWithCode(code = error.code, message = error.message)
                }
                    ?: BlockchainSdkError.CustomError("Unknown response format")
        }
        is Result.Failure -> {
            throw this.error as? BlockchainSdkError ?: BlockchainSdkError.CustomError("Unknown error format")
        }
    }

    internal companion object {

        /**
         * Decodes the SCALE-encoded `RuntimeDispatchInfo` returned by `TransactionPaymentApi_query_info`.
         *
         * Layout: `Compact<u64> refTime | Compact<u64> proofSize | u8 dispatchClass | partialFee`.
         *
         * The width of `partialFee` is runtime-dependent (u64 on Bittensor, u128 on standard Substrate)
         * and the leading `Compact<u64>` weight fields are themselves variable length, so the total
         * response size is not constant. A previous implementation branched on a hardcoded response
         * length (15 bytes); when the Bittensor finney runtime started reporting a larger `proofSize`
         * (crossing the 2^14 compact boundary, growing the response to 17 bytes) the check fell through
         * to the u128 path and `readUint128()` overran the buffer, surfacing as `Polkadot.Api` (2001).
         *
         * To be resilient to such weight changes we decode whatever bytes remain after the header as a
         * little-endian unsigned integer instead of assuming a fixed width or response length.
         */
        fun parseFee(resultHex: String, decimals: Int): BigDecimal {
            val bytesToParse = resultHex.remove(HEX_PREFIX).decodeFromHex().toByteArray()
            val reader = ScaleCodecReader(bytesToParse)
            val parsedData = PolkadotRuntimeDispatchInfo(
                refTime = reader.readCompactInt(),
                proofSize = reader.readCompactInt(),
                classType = reader.readByte(),
                partialFee = reader.readRemainingAsUnsignedLittleEndian(),
            )
            return parsedData.partialFee.toBigDecimal().movePointLeft(decimals)
        }

        private fun ScaleCodecReader.readRemainingAsUnsignedLittleEndian(): BigInteger {
            val littleEndianBytes = buildList { while (hasNext()) add(readByte()) }
            return if (littleEndianBytes.isEmpty()) {
                BigInteger.ZERO
            } else {
                BigInteger(1, littleEndianBytes.asReversed().toByteArray())
            }
        }
    }
}