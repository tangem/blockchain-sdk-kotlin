package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank
import org.stellar.sdk.Server

internal class StellarProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<StellarWrapperNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Stellar, Blockchain.StellarTestnet)

    override fun createProviders(blockchain: Blockchain): List<StellarWrapperNetworkProvider> {
        return if (blockchain.isTestnet()) {
            listOf(StellarNetwork.HorizonTestnet)
        } else {
            listOfNotNull(
                StellarNetwork.Horizon,
                config.nowNodeCredentials?.apiKey.letNotBlank(StellarNetwork::Nownodes),
            )
        }
            .map(::createWrapperProvider)
    }

    private fun createWrapperProvider(network: StellarNetwork): StellarWrapperNetworkProvider {
        return StellarWrapperNetworkProvider(server = Server(network.url), url = network.url)
    }
}