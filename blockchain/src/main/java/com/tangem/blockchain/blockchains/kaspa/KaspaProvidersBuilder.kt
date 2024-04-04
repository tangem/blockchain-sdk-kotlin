package com.tangem.blockchain.blockchains.kaspa

import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkProvider
import com.tangem.blockchain.blockchains.kaspa.network.KaspaRestApiNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_KASPA

internal class KaspaProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<KaspaNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Kaspa)

    override fun createProviders(blockchain: Blockchain): List<KaspaNetworkProvider> {
        return listOfNotNull(
            API_KASPA,
            config.kaspaSecondaryApiUrl?.letNotBlank { "$it/" },
        )
            .map(::KaspaRestApiNetworkProvider)
    }
}