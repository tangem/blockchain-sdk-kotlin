package com.tangem.blockchain.blockchains.koinos

import com.tangem.blockchain.blockchains.koinos.network.KoinosApi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder

internal object KoinosProviderBuilder : NetworkProvidersBuilder<KoinosApi>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Koinos, Blockchain.KoinosTestnet)

    override fun createProviders(blockchain: Blockchain): List<KoinosApi> {
        return with(blockchain) {
            if (isTestnet()) {
                listOf(
                    provider("https://harbinger-api.koinos.io/"),
                    provider("https://testnet.koinosblocks.com/"),
                )
            } else {
                listOf(
                    provider("https://api.koinos.io/"),
                    provider("https://api.koinosblocks.com/"),
                )
            }
        }
    }

    private fun Blockchain.provider(baseUrl: String, apiKey: String? = null): KoinosApi {
        return KoinosApi(baseUrl, isTestnet = isTestnet(), apiKey = apiKey)
    }
}