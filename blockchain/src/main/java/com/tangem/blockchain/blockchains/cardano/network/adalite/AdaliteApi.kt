package com.tangem.blockchain.blockchains.cardano.network.adalite

import com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse.AdaliteAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse.AdaliteUnspentOutputsResponse
import com.tangem.blockchain.blockchains.cardano.network.adalite.request.AdaliteSendBody
import retrofit2.http.*

internal interface AdaliteApi {

    @GET("api/addresses/summary/{address}")
    suspend fun getAddressData(@Path("address") address: String): AdaliteAddressResponse

    @Headers("Content-Type: application/json")
    @POST("api/bulk/addresses/utxo")
    suspend fun getUnspentOutputs(@Body addresses: List<String>): AdaliteUnspentOutputsResponse

    @Headers("Content-Type: application/json")
    @POST("api/v2/txs/signed")
    suspend fun sendTransaction(@Body body: AdaliteSendBody): String
}