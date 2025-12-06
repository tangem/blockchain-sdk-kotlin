package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class InkProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        android.util.Log.d("InkProvidersBuilder", "createProviders called for $blockchain")
        android.util.Log.d("InkProvidersBuilder", "providerTypes: $providerTypes")
        
        val providers = providerTypes.mapNotNull {
            android.util.Log.d("InkProvidersBuilder", "Processing provider type: $it")
            when (it) {
                is ProviderType.Public -> {
                    android.util.Log.d("InkProvidersBuilder", "Creating public provider with URL: ${it.url}")
                    EthereumJsonRpcProvider(baseUrl = it.url)
                }
                ProviderType.NowNodes -> {
                    android.util.Log.d("InkProvidersBuilder", "Attempting NowNodes provider")
                    ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://ink.nownodes.io/")
                }
                ProviderType.GetBlock -> {
                    android.util.Log.d("InkProvidersBuilder", "Attempting GetBlock provider, ink token: ${config.getBlockCredentials?.ink?.jsonRpc}")
                    ethereumProviderFactory.getGetBlockProvider { ink?.jsonRpc }
                }
                else -> {
                    android.util.Log.d("InkProvidersBuilder", "Unsupported provider type: $it")
                    null
                }
            }
        }
        
        android.util.Log.d("InkProvidersBuilder", "Total providers created: ${providers.size}")
        return providers
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc-gel-sepolia.inkonchain.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc-qnd-sepolia.inkonchain.com/"),
        )
    }
}

