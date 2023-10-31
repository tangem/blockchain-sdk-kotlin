package com.tangem.blockchain.blockchains.xrp.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Api for decoding and encoding XRP X Addresses
 *
 * @see [https://xrpaddress.info/](https://xrpaddress.info/) for more information
 */
interface XrpAddressInfoApi {
    @GET("/api/encode/{address}/{tag}")
    suspend fun encodeAddress(@Path("address") address: String, @Path("tag") tag: String): XRPAddressResult

    @GET("/api/decode/{address}")
    suspend fun decodeAddress(@Path("address") address: String): XRPAddressResult
}

//region XRP Address
@JsonClass(generateAdapter = true)
data class XRPAddressResult(
    @Json(name = "address")
    var address: String? = null,
    @Json(name = "account")
    var account: String? = null,
    @Json(name = "tag")
    var tag: String? = null,
)
//endregion





