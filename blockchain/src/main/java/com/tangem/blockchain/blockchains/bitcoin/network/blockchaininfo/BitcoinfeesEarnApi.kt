package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import retrofit2.http.GET

interface BitcoinfeesEarnApi {
    @GET("api/v1/fees/recommended")
    suspend fun getFees(): BitcoinfeesEarnResponse
}