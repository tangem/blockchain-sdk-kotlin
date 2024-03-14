package com.tangem.blockchain.network.electrum

import com.tangem.blockchain.common.Blockchain

internal fun Blockchain.getElectrumNetworkProviders(): List<ElectrumNetworkProvider> {
    return when (this) {
        Blockchain.Nexa -> listOf(
            createNetworkProvider(wssUrl = "wss://onekey-electrum.bitcoinunlimited.info:20004"),
            createNetworkProvider(wssUrl = "wss://electrum.nexa.org:20004"),
        )
        else -> error("$this is not supported")
    }
}

private fun Blockchain.createNetworkProvider(wssUrl: String): ElectrumNetworkProvider = DefaultElectrumNetworkProvider(
    baseUrl = wssUrl,
    blockchain = this,
)