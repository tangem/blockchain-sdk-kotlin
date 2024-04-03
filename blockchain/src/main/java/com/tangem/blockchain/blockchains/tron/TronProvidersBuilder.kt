package com.tangem.blockchain.blockchains.tron

import com.tangem.blockchain.blockchains.tron.network.TronJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tron.network.TronNetwork
import com.tangem.blockchain.blockchains.tron.network.TronNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank

internal class TronProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<TronNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Tron, Blockchain.TronTestnet)

    override fun createProviders(blockchain: Blockchain): List<TronNetworkProvider> {
        return if (blockchain.isTestnet()) {
            listOf(TronNetwork.Nile)
        } else {
            listOfNotNull(
                TronNetwork.TronGrid(apiKey = null),
                config.tronGridApiKey.letNotBlank(TronNetwork::TronGrid),
                config.nowNodeCredentials?.apiKey.letNotBlank(TronNetwork::NowNodes),
                config.getBlockCredentials?.tron?.rest.letNotBlank(TronNetwork::GetBlock),
            )
        }
            .map(::TronJsonRpcNetworkProvider)
    }
}