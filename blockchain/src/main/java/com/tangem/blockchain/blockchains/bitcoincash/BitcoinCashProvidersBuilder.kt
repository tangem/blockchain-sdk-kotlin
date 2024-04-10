package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProviderFactory

internal class BitcoinCashProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<BitcoinNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<BitcoinNetworkProvider> {
        return listOfNotNull(
            getBitcoinCashNowNodesNetworkProvider(),
            *BlockchairNetworkProviderFactory(config).createProviders(blockchain).toTypedArray(),
        )
    }

    private fun getBitcoinCashNowNodesNetworkProvider(): BitcoinNetworkProvider? {
        return config.nowNodeCredentials?.apiKey?.letNotBlank {
            BitcoinCashNowNodesNetworkProvider(
                credentials = NowNodeCredentials.headerApiKey to config.nowNodeCredentials.apiKey,
                bchBookUrl = "https://bchbook.nownodes.io/",
                bchUrl = "https://bch.nownodes.io/",
            )
        }
    }
}