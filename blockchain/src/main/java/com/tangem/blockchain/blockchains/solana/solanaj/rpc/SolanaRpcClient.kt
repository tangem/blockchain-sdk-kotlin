package com.tangem.blockchain.blockchains.solana.solanaj.rpc

import com.tangem.blockchain.common.NetworkProvider
import okhttp3.Interceptor
import org.p2p.solanaj.rpc.RpcClient

/**
[REDACTED_AUTHOR]
 */
internal class SolanaRpcClient(
    override val baseUrl: String,
    httpInterceptors: List<Interceptor>? = null,
) : RpcClient(baseUrl, httpInterceptors), NetworkProvider {

    override fun createRpcApi(): SolanaRpcApi = SolanaRpcApi(this)
    override fun getApi(): SolanaRpcApi = super.getApi() as SolanaRpcApi
}