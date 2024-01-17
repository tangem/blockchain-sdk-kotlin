package com.tangem.blockchain.blockchains.solana.solanaj.rpc

import okhttp3.Interceptor
import org.p2p.solanaj.rpc.RpcClient

/**
[REDACTED_AUTHOR]
 */
internal class SolanaRpcClient(
    val host: String,
    httpInterceptors: List<Interceptor>? = null,
) : RpcClient(host, httpInterceptors) {

    override fun createRpcApi(): SolanaRpcApi = SolanaRpcApi(this)
    override fun getApi(): SolanaRpcApi = super.getApi() as SolanaRpcApi
}