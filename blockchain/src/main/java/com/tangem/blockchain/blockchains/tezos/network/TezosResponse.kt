package com.tangem.blockchain.blockchains.tezos.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TezosAddressResponse(
    @Json(name = "balance")
    val balance: Long? = null,

    @Json(name = "counter")
    val counter: Long? = null,
)

@JsonClass(generateAdapter = true)
data class TezosHeaderResponse(
    @Json(name = "protocol")
    val protocol: String? = null,

    @Json(name = "hash")
    val hash: String? = null,
)