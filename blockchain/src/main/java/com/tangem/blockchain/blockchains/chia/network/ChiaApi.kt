package com.tangem.blockchain.blockchains.chia.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface ChiaApi {
    @POST("get_coin_records_by_puzzle_hash")
    suspend fun getUnspentsByPuzzleHash(@Body puzzleHash: ChiaPuzzleHashBody): ChiaCoinRecordsResponse

    @POST("get_fee_estimate")
    suspend fun getFeeEstimate(@Body transaction: ChiaFeeEstimateBody): ChiaEstimateFeeResponse

    @POST("push_tx")
    suspend fun sendTransaction(@Body transaction: ChiaTransactionBody): ChiaSendTransactionResponse
}

@JsonClass(generateAdapter = true)
data class ChiaPuzzleHashBody(
    @Json(name = "puzzle_hash")
    val puzzleHash: String
)

@JsonClass(generateAdapter = true)
data class ChiaFeeEstimateBody(
    @Json(name = "cost")
    val cost: Long,

    @Json(name = "target_times")
    val targetTimes: List<Int>
)

@JsonClass(generateAdapter = true)
data class ChiaTransactionBody(
    @Json(name = "spend_bundle")
    val spendBundle: ChiaSpendBundle
)

@JsonClass(generateAdapter = true)
data class ChiaSpendBundle(
    @Json(name = "aggregated_signature")
    val aggregatedSignature: String,

    @Json(name = "coin_spends")
    val coinSpends: List<ChiaCoinSpend>
)

@JsonClass(generateAdapter = true)
class ChiaCoinSpend(
    @Json(name = "coin")
    val coin: ChiaCoin,

    @Json(name = "puzzle_reveal")
    val puzzleReveal: String,

    @Json(name = "solution")
    var solution: String
)

@JsonClass(generateAdapter = true)
data class ChiaCoin(
    // Has to be encoded as a number in JSON, therefore Long is used. It's enough to encode ~1/3 of Chia total supply.
    @Json(name = "amount")
    val amount: Long,

    @Json(name = "parent_coin_info")
    val parentCoinInfo: String,

    @Json(name = "puzzle_hash")
    val puzzleHash: String,
)