package com.tangem.blockchain.blockchains.casper.network

import com.tangem.blockchain.blockchains.casper.network.response.CasperRpcResponse
import com.tangem.blockchain.common.JsonRPCRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Casper RPC API
 *
 * @see <a href="https://docs.casper.network/developers/json-rpc/">Casper JSON RPC</a>
 *
 */
internal interface CasperApi {

    /**
     * Get data by body [JsonRPCRequest]
     *
     * @param postfixUrl postfix url for supports base url without '/'
     * @param body       rpc body
     */
    @Headers("Content-Type: application/json")
    @POST
    suspend fun post(@Url postfixUrl: String, @Body body: JsonRPCRequest): CasperRpcResponse
}