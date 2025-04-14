package com.tangem.blockchain.blockchains.bitcoincash.api

import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
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
}