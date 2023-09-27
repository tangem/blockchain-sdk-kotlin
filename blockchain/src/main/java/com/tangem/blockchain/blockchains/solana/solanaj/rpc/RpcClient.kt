package com.tangem.blockchain.blockchains.solana.solanaj.rpc

import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import okhttp3.Interceptor
import org.p2p.solanaj.rpc.RpcClient

/**
[REDACTED_AUTHOR]
 */
class RpcClient(
    val host: String,
    httpInterceptors: List<Interceptor>? = BlockchainSdkRetrofitBuilder.interceptors,
) : RpcClient(host, httpInterceptors) {

    override fun createRpcApi(): RpcApi = RpcApi(this)

    override fun getApi(): RpcApi = super.getApi() as RpcApi
}