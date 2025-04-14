package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.blockbook.BlockBookNetworkProviderFactory
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProviderFactory

internal class BitcoinCashProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<BitcoinNetworkProvider>() {

    private val blockBookProviderFactory by lazy { BlockBookNetworkProviderFactory(config) }

    override fun createProviders(blockchain: Blockchain): List<BitcoinNetworkProvider> {
        return providerTypes.flatMap {
            when (it) {
                ProviderType.NowNodes -> getBitcoinCashNowNodesNetworkProvider().let(::listOfNotNull)
                ProviderType.GetBlock -> {
                    blockBookProviderFactory.createGetBlockProvider(blockchain).let(::listOfNotNull)
                }
                ProviderType.BitcoinLike.Blockchair -> {
                    BlockchairNetworkProviderFactory(config).createProviders(blockchain)
                }
                else -> emptyList()
            }
        }
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