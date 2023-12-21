package com.tangem.blockchain.blockchains.chia.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.AddHeaderInterceptor
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import java.math.RoundingMode

open class ChiaJsonRpcProvider(override val baseUrl: String, key: String) : ChiaNetworkProvider {

    private val api: ChiaApi by lazy {
        createRetrofitInstance(
            baseUrl = baseUrl,
            headerInterceptors = listOf(
                AddHeaderInterceptor(
                    headers = buildMap {
                        put("Content-Type", "application/json")
                        put("X-API-Key", key)
                    },
                )
            )
        ).create(ChiaApi::class.java)
    }

    private val decimals = Blockchain.Chia.decimals()

    override suspend fun getUnspents(puzzleHash: String): Result<List<ChiaCoin>> {
        return try {
            val uspentsData = retryIO { api.getUnspentsByPuzzleHash(ChiaPuzzleHashBody(puzzleHash)) }
            Result.Success(uspentsData.coinRecords.map { it.coin })
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getFeeEstimate(transactionCost: Long): Result<EstimateFeeResult> {
        return try {
            val feeData = retryIO {
                api.getFeeEstimate(ChiaFeeEstimateBody(transactionCost, ESTIMATE_FEE_TARGET_TIMES))
            }
            val feeList = listOf(
                feeData.estimates[0].toBigDecimal().movePointLeft(decimals),
                (feeData.feeRateLastBlock * transactionCost).toBigDecimal()
                    .setScale(0, RoundingMode.DOWN)
                    .movePointLeft(decimals)
            ).sorted()
            Result.Success(
                EstimateFeeResult(
                    normalFee = feeList[0],
                    priorityFee = feeList[1]
                )
            )
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: ChiaTransactionBody): SimpleResult {
        return try {
            val sendResponse = retryIO { api.sendTransaction(transaction) }
            if (sendResponse.success) {
                if (sendResponse.status == SUCCESS_STATUS) {
                    SimpleResult.Success
                } else {
                    SimpleResult.Failure(
                        BlockchainSdkError.CustomError("${sendResponse.status} status returned on send")
                    )
                }
            } else {
                SimpleResult.Failure(
                    BlockchainSdkError.CustomError(sendResponse.error ?: "Unknown send error")
                )
            }
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }

    companion object {
        // time in seconds, 1 minute only, 5 minutes gives bad estimate now
        private val ESTIMATE_FEE_TARGET_TIMES = listOf(60)

        private const val SUCCESS_STATUS = "SUCCESS"
    }
}