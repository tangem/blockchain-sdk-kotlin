package com.tangem.blockchain.blockchains.ducatus.network.bitcore

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BitcoreSendBody(@Json(name = "rawTx") val rawTx: List<String>)