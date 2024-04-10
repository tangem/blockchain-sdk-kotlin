package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank
import org.stellar.sdk.Server

internal class StellarProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<StellarWrapperNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<StellarWrapperNetworkProvider> {
        return providerTypes
            .mapNotNull {
                when (it) {
                    is ProviderType.Public -> StellarNetwork.Public(it.url)
                    ProviderType.NowNodes -> config.nowNodeCredentials?.apiKey.letNotBlank(StellarNetwork::Nownodes)
                    else -> null
                }
            }
            .map(::createWrapperProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<StellarWrapperNetworkProvider> {
        return listOf(
            createWrapperProvider(StellarNetwork.HorizonTestnet),
        )
    }

    private fun createWrapperProvider(network: StellarNetwork): StellarWrapperNetworkProvider {
        return StellarWrapperNetworkProvider(server = Server(network.url), url = network.url)
    }
}