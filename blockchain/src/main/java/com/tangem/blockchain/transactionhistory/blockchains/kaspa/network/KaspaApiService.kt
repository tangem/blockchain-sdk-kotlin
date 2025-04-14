package com.tangem.blockchain.transactionhistory.blockchains.kaspa.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Kaspa blockchain API
 */
internal interface KaspaApiService {

    /**
     * Get full transactions for a specific Kaspa address
     *
     * @param address The Kaspa address to query (URL encoded)
     * @param limit Maximum number of transactions to return
     * @param offset Number of transactions to skip for pagination
     * @param resolvePreviousOutpoints Level of detail for previous outpoints ("light", "full", or "none")
     * @return Call object containing the transaction response
     */
    @GET("addresses/{address}/full-transactions")
    suspend fun getAddressTransactions(
        @Path("address") address: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("resolve_previous_outpoints") resolvePreviousOutpoints: String = "light",
    ): List<KaspaCoinTransaction>
}