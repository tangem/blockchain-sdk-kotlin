package com.tangem.blockchain.network.blockbook.network.responses

data class GetFeeResponse(
    val id: String,
    val result: Result
) {
    data class Result(
        val blocks: Int,
        val feerate: Double
    )
}