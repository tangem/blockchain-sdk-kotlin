package com.tangem.blockchain.blockchains.chia.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import java.math.BigDecimal
import java.math.RoundingMode

open class ChiaJsonRpcProvider(
    override val baseUrl: String,
    private val isRequiredHexPrefixForTx: Boolean,
    key: String,
) :
    ChiaNetworkProvider {

    private val api: ChiaApi by lazy {
        createRetrofitInstance(
            baseUrl = baseUrl,
            headerInterceptors = listOf(
                AddHeaderInterceptor(
                    headers = buildMap {
                        put("Content-Type", "application/json")
                        put("X-API-Key", key)
                    },
                ),
            ),
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

    override suspend fun getFeeEstimate(transactionCost: Long): Result<ChiaEstimateFeeResult> {
        return try {
            val feeData = retryIO {
                api.getFeeEstimate(ChiaFeeEstimateBody(transactionCost, ESTIMATE_FEE_TARGET_TIMES))
            }
            val baseFee = listOf(
                feeData.estimates[0].toBigDecimal(),
                (feeData.feeRateLastBlock * transactionCost).toBigDecimal().setScale(0, RoundingMode.DOWN),
            ).max().movePointLeft(decimals)

            Result.Success(
                ChiaEstimateFeeResult(
                    minimalFee = (baseFee * minimalFeeMultiplier).setScale(decimals, RoundingMode.DOWN),
                    normalFee = (baseFee * normalFeeMultiplier).setScale(decimals, RoundingMode.DOWN),
                    priorityFee = (baseFee * priorityFeeMultiplier).setScale(decimals, RoundingMode.DOWN),
                ),
            )
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: ChiaTransactionBody): SimpleResult {
        return try {
            val tx = if (isRequiredHexPrefixForTx) {
                transaction.appendHexPrefix()
            } else {
                transaction
            }

            val sendResponse = retryIO { api.sendTransaction(tx) }
            if (sendResponse.success) {
                if (sendResponse.status == SUCCESS_STATUS) {
                    SimpleResult.Success
                } else {
                    SimpleResult.Failure(
                        BlockchainSdkError.CustomError("${sendResponse.status} status returned on send"),
                    )
                }
            } else {
                SimpleResult.Failure(
                    BlockchainSdkError.CustomError(sendResponse.error ?: "Unknown send error"),
                )
            }
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }

    private fun ChiaTransactionBody.appendHexPrefix(): ChiaTransactionBody {
        val aggregatedSignature = spendBundle.aggregatedSignature
        val coinSpends = spendBundle.coinSpends
        return copy(
            spendBundle = spendBundle.copy(
                aggregatedSignature = "$HEX_PREFIX$aggregatedSignature",
                coinSpends = coinSpends.map {
                    ChiaCoinSpend(
                        coin = it.coin,
                        puzzleReveal = "$HEX_PREFIX${it.puzzleReveal}",
                        solution = "$HEX_PREFIX${it.solution}",
                    )
                },
            ),
        )
    }

    companion object {
        // time in seconds, 1 minute only, 5 minutes gives bad estimate now
        private val ESTIMATE_FEE_TARGET_TIMES = listOf(60)

        private const val SUCCESS_STATUS = "SUCCESS"

        val minimalFeeMultiplier: BigDecimal = BigDecimal("1.5")
        val normalFeeMultiplier: BigDecimal = BigDecimal("2")
        val priorityFeeMultiplier: BigDecimal = BigDecimal("5")
    }
}