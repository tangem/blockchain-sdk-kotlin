package com.tangem.blockchain.blockchains.sui

import com.tangem.blockchain.blockchains.sui.network.rpc.SuiJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class SuiNetworkProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : NetworkProvidersBuilder<SuiJsonRpcProvider>() {

    override fun createProviders(blockchain: Blockchain): List<SuiJsonRpcProvider> {
        return providerTypes.mapNotNull { type ->
            val url = when (type) {
                is ProviderType.Public -> type.url
                else -> null
            }

            if (url != null) SuiJsonRpcProvider(url) else null
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<SuiJsonRpcProvider> {
        return listOf(
            SuiJsonRpcProvider(baseUrl = "https://fullnode.testnet.sui.io/"),
        )
    }
}