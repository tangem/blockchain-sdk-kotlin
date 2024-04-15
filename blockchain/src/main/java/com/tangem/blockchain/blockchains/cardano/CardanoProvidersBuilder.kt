package com.tangem.blockchain.blockchains.cardano

import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.adalite.AdaliteNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.rosetta.RosettaNetwork
import com.tangem.blockchain.blockchains.cardano.network.rosetta.RosettaNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_ADALITE

internal class CardanoProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<CardanoNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<CardanoNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                ProviderType.GetBlock -> createGetBlockNetworkProvider()
                ProviderType.Cardano.Adalite -> AdaliteNetworkProvider(API_ADALITE)
                ProviderType.Cardano.Rosetta -> RosettaNetworkProvider(RosettaNetwork.Tangem)
                else -> null
            }
        }
    }

    private fun createGetBlockNetworkProvider(): CardanoNetworkProvider? {
        return config.getBlockCredentials?.cardano?.rosetta.letNotBlank {
            RosettaNetworkProvider(RosettaNetwork.Getblock(it))
        }
    }
}