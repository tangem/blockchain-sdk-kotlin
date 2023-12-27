package com.tangem.blockchain.network.blockbook.network.responses

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendTransactionResponse(val result: String)