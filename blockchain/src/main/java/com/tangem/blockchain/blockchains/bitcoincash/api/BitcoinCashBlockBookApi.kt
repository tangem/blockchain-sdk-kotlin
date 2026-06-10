package com.tangem.blockchain.blockchains.bitcoincash.api

import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
import com.tangem.blockchain.network.blockbook.network.responses.GetXpubResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response

internal interface BitcoinCashBlockBookApi {

    @GET("api/v2/address/{address}")
    suspend fun getAddress(
        @Path("address") address: String,
        @Query("details") details: String = "txs",
    ): Response<GetAddressResponse>

    @GET("api/v2/utxo/{address}")
    suspend fun getUtxo(@Path("address") address: String): Response<List<GetUtxoResponseItem>>

    // `encoded = true` is mandatory: descriptor strings like `pkh(<xpub>)` contain `(` and `)`
    // which Retrofit would otherwise %-encode and Blockbook returns 404.
    @GET("api/v2/xpub/{descriptor}")
    suspend fun getXpubInfo(
        @Path(value = "descriptor", encoded = true) descriptor: String,
        @Query("details") details: String = "txslight",
        @Query("tokens") tokens: String = "used",
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
    ): Response<GetXpubResponse>

    @GET("api/v2/utxo/{descriptor}")
    suspend fun getXpubUtxo(
        @Path(value = "descriptor", encoded = true) descriptor: String,
    ): Response<List<GetUtxoResponseItem>>
}