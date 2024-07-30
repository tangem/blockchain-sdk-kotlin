package com.tangem.blockchain.blockchains.filecoin.network

import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinRpcBody
import com.tangem.blockchain.blockchains.filecoin.network.response.FilecoinRpcResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Filecoin RPC API
 *
 * @see <a href="https://docs.filecoin.io/reference/json-rpc">Filecoin JSON RPC</a>
 *
 * @author Andrew Khokhlov on 24/07/2024
 */
internal interface FilecoinApi {

    /**
     * Get data by body [FilecoinRpcBody]
     *
     * @param postfixUrl postfix url for supports base url without '/'
     * @param body       rpc body
     */
    @POST
    suspend fun post(@Url postfixUrl: String, @Body body: FilecoinRpcBody): FilecoinRpcResponse
}
