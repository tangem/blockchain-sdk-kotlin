package com.tangem.blockchain.blockchains.cosmos

import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class CosmosProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<CosmosRestProvider>() {

    override fun createProviders(blockchain: Blockchain): List<CosmosRestProvider> {
        return listOfNotNull(
            createNowNodesProvider(),
            createGetBlockProvider(),
            "https://cosmos-mainnet-rpc.allthatnode.com:1317/",
            // This is a REST proxy combining the servers below (and others)
            "https://rest.cosmos.directory/cosmoshub/",
            "https://cosmoshub-api.lavenderfive.com/",
            "https://rest-cosmoshub.ecostake.com/",
            "https://lcd.cosmos.dragonstake.io/",
        )
            .map(::CosmosRestProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<CosmosRestProvider> {
        return listOf("https://rest.seed-01.theta-testnet.polypore.xyz")
            .map(::CosmosRestProvider)
    }

    private fun createNowNodesProvider(): String? {
        return config.nowNodeCredentials?.apiKey.letNotBlank { "https://atom.nownodes.io/$it/" }
    }

    private fun createGetBlockProvider(): String? {
        return config.getBlockCredentials?.cosmos?.rest.letNotBlank { "https://go.getblock.io/$it/" }
    }
}