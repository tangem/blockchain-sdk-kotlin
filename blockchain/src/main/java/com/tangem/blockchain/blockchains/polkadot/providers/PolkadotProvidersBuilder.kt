package com.tangem.blockchain.blockchains.polkadot.providers

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class PolkadotProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<PolkadotNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<PolkadotNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.GetBlock -> createGetBlockProvider(blockchain)
                is ProviderType.Public -> createPublicProvider(it.url, blockchain)
                is ProviderType.PolkadotLike.Tatum -> createTatumProvider(blockchain)
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<PolkadotNetworkProvider> {
        return listOf(createPublicProvider("https://westend-rpc.polkadot.io/", blockchain))
    }

    private fun createPublicProvider(url: String, blockchain: Blockchain): PolkadotNetworkProvider {
        return PolkadotCombinedProvider(baseUrl = url, blockchain = blockchain)
    }

    private fun createGetBlockProvider(blockchain: Blockchain): PolkadotNetworkProvider? {
        return config.getBlockCredentials?.polkadot?.jsonRpc.letNotBlank { jsonRpcToken ->
            PolkadotCombinedProvider(baseUrl = "https://go.getblock.io/$jsonRpcToken/", blockchain = blockchain)
        }
    }

    private fun createTatumProvider(blockchain: Blockchain): PolkadotNetworkProvider? {
        return config.tatumApiKey?.letNotBlank { tatumApiKey ->
            PolkadotCombinedProvider(
                baseUrl = "https://polkadot-assethub.gateway.tatum.io/",
                blockchain = blockchain,
                credentials = mapOf("x-api-key" to tatumApiKey),
            )
        }
    }
}