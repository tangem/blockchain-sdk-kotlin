package com.tangem.blockchain.network.electrum

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchain.network.electrum.api.DefaultElectrumApiService
import com.tangem.blockchain.network.electrum.api.ElectrumApiService
import com.tangem.blockchain.network.jsonrpc.DefaultJsonRPCService
import com.tangem.blockchain.network.jsonrpc.DefaultJsonRPCWebsocketService
import okhttp3.OkHttpClient
import org.jetbrains.annotations.ApiStatus.Experimental

internal object ElectrumNetworkProviderFactory {

    fun create(
        wssUrl: String,
        blockchain: Blockchain,
        supportedProtocolVersion: String = ElectrumApiService.SUPPORTED_PROTOCOL_VERSION,
        okHttpClient: OkHttpClient = BlockchainSdkRetrofitBuilder.build(),
    ): ElectrumNetworkProvider = DefaultElectrumNetworkProvider(
        baseUrl = wssUrl,
        blockchain = blockchain,
        service = DefaultElectrumApiService(
            rpcService = DefaultJsonRPCWebsocketService(
                wssUrl = wssUrl,
                pingPongRequestFactory = {
                    JsonRPCRequest(
                        method = "server.ping",
                        id = "keepAlive",
                        params = emptyList<String>(),
                    )
                },
                okHttpClient = okHttpClient,
            ),
        ),
        supportedProtocolVersion = supportedProtocolVersion,
    )

    @Experimental
    fun createHttpsVersion(
        httpsUrl: String,
        blockchain: Blockchain,
        supportedProtocolVersion: String = ElectrumApiService.SUPPORTED_PROTOCOL_VERSION,
        okHttpClient: OkHttpClient = BlockchainSdkRetrofitBuilder.build(),
    ): ElectrumNetworkProvider = DefaultElectrumNetworkProvider(
        baseUrl = httpsUrl,
        blockchain = blockchain,
        service = DefaultElectrumApiService(
            rpcService = DefaultJsonRPCService(
                url = httpsUrl,
                okHttpClient = okHttpClient,
            ),
        ),
        supportedProtocolVersion = supportedProtocolVersion,
    )
}