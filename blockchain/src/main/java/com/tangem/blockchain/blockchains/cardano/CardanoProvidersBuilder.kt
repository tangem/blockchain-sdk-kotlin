package com.tangem.blockchain.blockchains.cardano

import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.adalite.AdaliteNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.rosetta.RosettaNetwork
import com.tangem.blockchain.blockchains.cardano.network.rosetta.RosettaNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_ADALITE

internal class CardanoProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<CardanoNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<CardanoNetworkProvider> {
        return listOfNotNull(
            config.getBlockCredentials?.cardano?.rosetta.letNotBlank {
                RosettaNetworkProvider(RosettaNetwork.Getblock(it))
            },
            AdaliteNetworkProvider(API_ADALITE),
            RosettaNetworkProvider(RosettaNetwork.Tangem),
        )
    }
}