package com.tangem.blockchain.blockchains.polkadot.network

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

class PolkadotJsonRpcProvider(baseUrl: String) {

    val host: String = baseUrl

    private val api = createRetrofitInstance(baseUrl).create(PolkadotApi::class.java)

    @Throws
    suspend fun getFee(transaction: ByteArray, decimals: Int): BigDecimal {
        val response = PolkadotBody(
            PolkadotMethod.GET_FEE.method,
            listOf("0x" + transaction.toHexString()),
        ).post()
        return response.extractResult().getFee(decimals)
    }


    private suspend fun PolkadotBody.post(): Result<PolkadotResponse> {
        return try {
            val result = retryIO { api.post(this) }
            Result.Success(result)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private fun Result<PolkadotResponse>.extractResult(): Map<String, Any> =
        when (this) {
            is Result.Success -> {
                this.data.result
                    ?: throw this.data.error?.toException()?.toBlockchainSdkError()
                        ?: BlockchainSdkError.CustomError("Unknown response format")
            }

            is Result.Failure -> {
                throw (this.error as? BlockchainSdkError)
                    ?: BlockchainSdkError.CustomError("Unknown error format")
            }
        }

    private fun Map<String, Any>.getFee(decimals: Int): BigDecimal {
        val feeString = this[FEE] as? String
        return feeString?.toBigDecimal()?.movePointLeft(decimals) ?: BigDecimal.ZERO
    }

    private fun PolkadotError.toException() =
        Exception("Code: ${this.code}, ${this.message}")

    companion object {
        const val FEE = "partialFee"
    }
}