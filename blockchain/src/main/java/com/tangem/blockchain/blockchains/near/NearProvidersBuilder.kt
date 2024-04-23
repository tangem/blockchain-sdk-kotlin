package com.tangem.blockchain.blockchains.near

import com.tangem.blockchain.blockchains.near.network.NearJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.near.network.NearNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class NearProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<NearNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<NearNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> NearJsonRpcNetworkProvider(baseUrl = it.url)
                ProviderType.NowNodes -> createNowNodeJsonRpcProvider()
                ProviderType.GetBlock -> createGetBlockJsonRpcProvider()
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<NearNetworkProvider> {
        return listOf(
            NearJsonRpcNetworkProvider(baseUrl = "https://rpc.testnet.near.org/"),
        )
    }

    private fun createNowNodeJsonRpcProvider(): NearNetworkProvider? {
        return config.nowNodeCredentials?.apiKey.letNotBlank {
            NearJsonRpcNetworkProvider(baseUrl = "https://near.nownodes.io/$it/")
        }
    }

    private fun createGetBlockJsonRpcProvider(): NearNetworkProvider? {
        return config.getBlockCredentials?.near?.jsonRpc.letNotBlank {
            NearJsonRpcNetworkProvider(baseUrl = "https://go.getblock.io/$it/")
        }
    }
}