package com.tangem.blockchain.blockchains.vechain

import com.tangem.blockchain.blockchains.vechain.network.VeChainNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank

internal class VeChainProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<VeChainNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.VeChain, Blockchain.VeChainTestnet)

    override fun createProviders(blockchain: Blockchain): List<VeChainNetworkProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                "https://testnet.vecha.in/",
                "https://sync-testnet.vechain.org/",
                "https://testnet.veblocks.net/",
                "https://testnetc1.vechain.network/",
            )
        } else {
            listOfNotNull(
                config.nowNodeCredentials?.apiKey.letNotBlank { "https://vet.nownodes.io/$it/" },
                "https://mainnet.vecha.in/",
                "https://sync-mainnet.vechain.org/",
                "https://mainnet.veblocks.net/",
                "https://mainnetc1.vechain.network/",
                "https://us.node.vechain.energy/",
            )
        }
            .map(::VeChainNetworkProvider)
    }
}