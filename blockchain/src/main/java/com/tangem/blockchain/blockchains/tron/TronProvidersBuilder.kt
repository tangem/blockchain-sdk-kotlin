package com.tangem.blockchain.blockchains.tron

import com.tangem.blockchain.blockchains.tron.network.TronJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tron.network.TronNetwork
import com.tangem.blockchain.blockchains.tron.network.TronNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class TronProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<TronNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<TronNetworkProvider> {
        return providerTypes
            .mapNotNull {
                when (it) {
                    is ProviderType.Public -> TronNetwork.PublicTronGrid(baseUrl = it.url)
                    ProviderType.Tron.TronGrid -> createTronGridProvider()
                    ProviderType.NowNodes -> createNowNodesProvider()
                    ProviderType.GetBlock -> createGetBlockProvider()
                    else -> null
                }
            }
            .map(::TronJsonRpcNetworkProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<TronNetworkProvider> {
        return listOf(
            TronJsonRpcNetworkProvider(network = TronNetwork.Nile),
        )
    }

    private fun createTronGridProvider(): TronNetwork? {
        return config.tronGridApiKey.letNotBlank(TronNetwork::TronGrid)
    }

    private fun createNowNodesProvider(): TronNetwork? {
        return config.nowNodeCredentials?.apiKey.letNotBlank(TronNetwork::NowNodes)
    }

    private fun createGetBlockProvider(): TronNetwork? {
        return config.getBlockCredentials?.tron?.rest.letNotBlank(TronNetwork::GetBlock)
    }
}