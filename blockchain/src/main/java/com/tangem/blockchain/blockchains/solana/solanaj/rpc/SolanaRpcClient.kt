package com.tangem.blockchain.blockchains.solana.solanaj.rpc

import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.NetworkProvider
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.p2p.solanaj.rpc.RpcClient
import org.p2p.solanaj.rpc.RpcException
import org.p2p.solanaj.rpc.types.RpcRequest
import java.io.IOException
import javax.net.ssl.SSLHandshakeException

/**
[REDACTED_AUTHOR]
 */
internal class SolanaRpcClient(
    override val baseUrl: String,
    httpInterceptors: List<Interceptor>? = null,
) : RpcClient(baseUrl, httpInterceptors), NetworkProvider {

    private val internalHttpClient: OkHttpClient = createHttpClient(httpInterceptors)
    private val moshi = Moshi.Builder().build()
    private val requestAdapter = moshi.adapter(RpcRequest::class.java)

    override fun createRpcApi(): SolanaRpcApi = SolanaRpcApi(this)
    override fun getApi(): SolanaRpcApi = super.getApi() as SolanaRpcApi

    fun call(method: String?, params: List<Any>?): String {
        val rpcRequest = RpcRequest(method, params)
        val request =
            Request.Builder().url(this.endpoint).post(requestAdapter.toJson(rpcRequest).toRequestBody(JSON)).build()

        try {
            val response = internalHttpClient.newCall(request).execute()
            return response.body!!.string()
        } catch (e: SSLHandshakeException) {
            throw RpcException(e.message)
        } catch (e: IOException) {
            throw RpcException(e.message)
        }
    }

    companion object {
        val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    }
}