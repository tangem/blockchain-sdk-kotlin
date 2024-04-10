package com.tangem.blockchain.blockchains.vechain

import com.tangem.blockchain.blockchains.vechain.network.VeChainNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class VeChainProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<VeChainNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<VeChainNetworkProvider> {
        return listOfNotNull(
            createNowNodesProvider(),
            "https://mainnet.vecha.in/",
            "https://sync-mainnet.vechain.org/",
            "https://mainnet.veblocks.net/",
            "https://mainnetc1.vechain.network/",
            "https://us.node.vechain.energy/",
        )
            .map(::VeChainNetworkProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<VeChainNetworkProvider> {
        return listOf(
            "https://testnet.vecha.in/",
            "https://sync-testnet.vechain.org/",
            "https://testnet.veblocks.net/",
            "https://testnetc1.vechain.network/",
        )
            .map(::VeChainNetworkProvider)
    }

    private fun createNowNodesProvider(): String? {
        return config.nowNodeCredentials?.apiKey.letNotBlank { "https://vet.nownodes.io/$it/" }
    }
}