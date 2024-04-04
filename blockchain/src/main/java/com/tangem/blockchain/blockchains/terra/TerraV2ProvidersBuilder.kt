package com.tangem.blockchain.blockchains.terra

import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank

internal class TerraV2ProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<CosmosRestProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.TerraV2)

    override fun createProviders(blockchain: Blockchain): List<CosmosRestProvider> {
        return listOfNotNull(
            config.nowNodeCredentials?.apiKey.letNotBlank { "https://luna.nownodes.io/$it/" },
            "https://phoenix-lcd.terra.dev/",
        )
            .map(::CosmosRestProvider)
    }
}