package com.tangem.blockchain.network.blockbook.network.requests

class GetFeeRequest private constructor(
    val jsonrpc: String,
    val id: String,
    val method: String,
    val params: List<Int>
) {
    companion object {
        val FEE = GetFeeRequest(
            jsonrpc = "2.0",
            id = "id",
            method = "estimatesmartfee",
            params = listOf(1000)
        )
    }
}