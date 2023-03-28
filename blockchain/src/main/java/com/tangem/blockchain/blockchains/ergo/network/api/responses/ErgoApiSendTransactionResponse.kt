package com.tangem.blockchain.blockchains.ergo.network.api.responses

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ErgoApiSendTransactionResponse(
    val id: String
)