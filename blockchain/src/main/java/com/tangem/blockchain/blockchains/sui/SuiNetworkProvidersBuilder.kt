package com.tangem.blockchain.blockchains.sui

import com.tangem.blockchain.blockchains.sui.network.rpc.SuiJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class SuiNetworkProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<SuiJsonRpcProvider>() {

    override fun createProviders(blockchain: Blockchain): List<SuiJsonRpcProvider> {
        return providerTypes.mapNotNull { type ->
            val url = when (type) {
                is ProviderType.Public -> type.url
                is ProviderType.NowNodes -> createNowNodesProvider()
                is ProviderType.GetBlock -> createGetBlockProvider()
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

    private fun createNowNodesProvider(): String? {
        return config.nowNodeCredentials?.apiKey?.letNotBlank { "https://sui.nownodes.io/$it/" }
    }

    private fun createGetBlockProvider(): String? {
        return config.getBlockCredentials?.sui?.rest.letNotBlank { "https://go.getblock.io/$it/" }
    }
}