package com.tangem.blockchain.blockchains.ducatus.network.bitcore

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BitcoreSendBody(val rawTx: List<String>)