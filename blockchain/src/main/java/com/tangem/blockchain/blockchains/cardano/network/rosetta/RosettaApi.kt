package com.tangem.blockchain.blockchains.cardano.network.rosetta

import com.tangem.blockchain.blockchains.cardano.network.rosetta.request.*
import com.tangem.blockchain.blockchains.cardano.network.rosetta.response.RosettaCoinsResponse
import com.tangem.blockchain.blockchains.cardano.network.rosetta.response.RosettaSubmitResponse
import retrofit2.http.Body
import retrofit2.http.POST

internal interface RosettaApi {

    @POST("account/coins")
    suspend fun getCoins(@Body body: RosettaCoinsBody): RosettaCoinsResponse

    @POST("construction/submit")
    suspend fun submitTransaction(@Body body: RosettaSubmitBody): RosettaSubmitResponse
}