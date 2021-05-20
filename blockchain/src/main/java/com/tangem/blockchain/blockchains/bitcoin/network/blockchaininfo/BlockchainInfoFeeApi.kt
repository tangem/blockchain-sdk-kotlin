package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import retrofit2.http.GET

interface BlockchainInfoFeeApi {
    @GET("mempool/fees")
    suspend fun getFees(): BlockchainInfoFees
}