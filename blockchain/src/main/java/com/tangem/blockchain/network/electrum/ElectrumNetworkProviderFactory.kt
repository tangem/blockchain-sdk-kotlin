package com.tangem.blockchain.network.electrum

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchain.network.electrum.api.ElectrumApiService
import com.tangem.blockchain.network.electrum.api.WebSocketElectrumApiService
import okhttp3.OkHttpClient

internal object ElectrumNetworkProviderFactory {

    fun create(
        wssUrl: String,
        blockchain: Blockchain,
        supportedProtocolVersion: String = ElectrumApiService.SUPPORTED_PROTOCOL_VERSION,
        okHttpClient: OkHttpClient = BlockchainSdkRetrofitBuilder.build(),
    ): ElectrumNetworkProvider = DefaultElectrumNetworkProvider(
        baseUrl = wssUrl,
        blockchain = blockchain,
        service = WebSocketElectrumApiService(wssUrl = wssUrl, okHttpClient = okHttpClient),
        supportedProtocolVersion = supportedProtocolVersion,
    )
}