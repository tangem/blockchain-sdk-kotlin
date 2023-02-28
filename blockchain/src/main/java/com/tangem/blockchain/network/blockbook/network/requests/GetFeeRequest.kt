package com.tangem.blockchain.network.blockbook.network.requests

class GetFeeRequest private constructor(
    val jsonrpc: String,
    val id: String,
    val method: String,
    val params: List<Int>
) {
    companion object {

        fun getFee(param: Int): GetFeeRequest {
            return GetFeeRequest(
                jsonrpc = "2.0",
                id = "id",
                method = "estimatesmartfee",
                params = listOf(param)
            )
        }
    }
}