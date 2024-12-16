package com.tangem.blockchain.blockchains.tezos

import com.tangem.blockchain.blockchains.tezos.network.TezosJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class TezosProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<TezosNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<TezosNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> createPublicProvider(url = it.url)
                ProviderType.GetBlock -> createGetBlockProvider()
                else -> null
            }
        }
    }

    private fun createPublicProvider(url: String) = TezosJsonRpcNetworkProvider(url)

    private fun createGetBlockProvider(): TezosJsonRpcNetworkProvider? {
        return config.getBlockCredentials?.tezos?.rest.letNotBlank {
            TezosJsonRpcNetworkProvider(baseUrl = "https://go.getblock.io/$it/")
        }
    }
}