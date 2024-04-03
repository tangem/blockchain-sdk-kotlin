package com.tangem.blockchain.blockchains.cosmos

import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank

internal class CosmosProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<CosmosRestProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Cosmos, Blockchain.CosmosTestnet)

    override fun createProviders(blockchain: Blockchain): List<CosmosRestProvider> {
        return if (blockchain.isTestnet()) {
            listOf("https://rest.seed-01.theta-testnet.polypore.xyz")
        } else {
            listOfNotNull(
                config.nowNodeCredentials?.apiKey.letNotBlank { "https://atom.nownodes.io/$it/" },
                config.getBlockCredentials?.cosmos?.rest.letNotBlank { "https://go.getblock.io/$it/" },
                "https://cosmos-mainnet-rpc.allthatnode.com:1317/",
                // This is a REST proxy combining the servers below (and others)
                "https://rest.cosmos.directory/cosmoshub/",
                "https://cosmoshub-api.lavenderfive.com/",
                "https://rest-cosmoshub.ecostake.com/",
                "https://lcd.cosmos.dragonstake.io/",
            )
        }
            .map(::CosmosRestProvider)
    }
}