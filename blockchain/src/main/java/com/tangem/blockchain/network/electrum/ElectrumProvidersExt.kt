package com.tangem.blockchain.network.electrum

import com.tangem.blockchain.blockchains.radiant.network.RadiantNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchain.network.electrum.api.ElectrumApiService
import com.tangem.blockchain.network.electrum.api.WebSocketElectrumApiService
import okhttp3.OkHttpClient

internal fun Blockchain.getElectrumNetworkProviders(): List<ElectrumNetworkProvider> {
    return when (this) {
        Blockchain.Nexa -> listOf(
            createNetworkProvider(wssUrl = "wss://onekey-electrum.bitcoinunlimited.info:20004"),
            createNetworkProvider(wssUrl = "wss://electrum.nexa.org:20004"),
        )
        Blockchain.Radiant -> listOf(
            createNetworkProvider(
                wssUrl = "wss://electrumx-01-ssl.radiant4people.com:51002",
                supportedProtocolVersion = RadiantNetworkService.SUPPORTED_SERVER_VERSION,
                okHttpClient = BlockchainSdkRetrofitBuilder.createOkhttpClientForRadiant(),
            ),
            createNetworkProvider(
                wssUrl = "wss://electrumx-02-ssl.radiant4people.com:51002",
                supportedProtocolVersion = RadiantNetworkService.SUPPORTED_SERVER_VERSION,
                okHttpClient = BlockchainSdkRetrofitBuilder.createOkhttpClientForRadiant(),
            ),
        )
        else -> error("$this is not supported")
    }
}

private fun Blockchain.createNetworkProvider(
    wssUrl: String,
    supportedProtocolVersion: String = ElectrumApiService.SUPPORTED_PROTOCOL_VERSION,
    okHttpClient: OkHttpClient = BlockchainSdkRetrofitBuilder.build(),
): ElectrumNetworkProvider = DefaultElectrumNetworkProvider(
    baseUrl = wssUrl,
    blockchain = this,
    service = WebSocketElectrumApiService(wssUrl = wssUrl, okHttpClient = okHttpClient),
    supportedProtocolVersion = supportedProtocolVersion,
)