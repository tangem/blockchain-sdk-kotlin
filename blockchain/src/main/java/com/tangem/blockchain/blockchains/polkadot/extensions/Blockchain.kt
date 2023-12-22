package com.tangem.blockchain.blockchains.polkadot.extensions

import com.tangem.blockchain.common.Blockchain

internal fun Blockchain.getPolkadotHosts(): List<String> {
    return when (this) {
        Blockchain.Polkadot -> listOf(
            "https://rpc.polkadot.io/",
            "https://polkadot.api.onfinality.io/public-ws/",
            "https://polkadot-rpc.dwellir.com/",
        )
        Blockchain.PolkadotTestnet -> listOf(
            "https://westend-rpc.polkadot.io/",
        )
        Blockchain.Kusama -> listOf(
            "https://kusama-rpc.polkadot.io/",
            "https://kusama.api.onfinality.io/public-ws/",
            "https://kusama-rpc.dwellir.com/",
        )
        Blockchain.AlephZero -> listOf(
            "https://rpc.azero.dev/",
            "https://aleph-zero-rpc.dwellir.com/",
        )
        Blockchain.AlephZeroTestnet -> listOf(
            "https://rpc.test.azero.dev",
        )
        else -> error("$this isn't supported")
    }
}
