package com.tangem.blockchain.network.blockchair

import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface BlockchairApi {
    @GET("{blockchain}/dashboards/address/{address}")
    suspend fun getAddressData(
            @Path("address") address: String,
            @Path("blockchain") blockchain: String,
            @Query("transaction_details") transactionDetails: Boolean = false,
            @Query("limit") limit: Int? = null,
            @Query("key") key: String = API_KEY
    ): BlockchairAddress

    @GET("{blockchain}/dashboards/transaction/{transaction}")
    suspend fun getTransaction(
            @Path("transaction") transactionHash: String,
            @Path("blockchain") blockchain: String,
            @Query("key") key: String = API_KEY
    ): BlockchairTransaction

    @GET("{blockchain}/stats")
    suspend fun getBlockchainStats(
            @Path("blockchain") blockchain: String,
            @Query("key") key: String = API_KEY
    ): BlockchairStats

    @POST("{blockchain}/push/transaction")
    suspend fun sendTransaction(
            @Body sendBody: BlockchairBody,
            @Path("blockchain") blockchain: String,
            @Query("key") key: String = API_KEY
    )

    @GET("{blockchain}/erc-20/{contract_address}/dashboards/address/{address}")
    suspend fun getTokenHolderData(
            @Path("address") address: String,
            @Path("blockchain") blockchain: String,
            @Path("contract_address") contractAddress: String,
            @Query("limit") limit: Int? = null,
            @Query("key") key: String = API_KEY
    ): BlockchairTokenHolder
}

@JsonClass(generateAdapter = true)
data class BlockchairBody(val data: String)

private const val API_KEY = "A___0Shpsu4KagE7oSabrw20DfXAqWlT"