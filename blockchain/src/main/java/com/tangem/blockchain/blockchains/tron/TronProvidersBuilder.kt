package com.tangem.blockchain.blockchains.tron

import com.tangem.blockchain.blockchains.tron.network.TronJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tron.network.TronNetwork
import com.tangem.blockchain.blockchains.tron.network.TronNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class TronProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<TronNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<TronNetworkProvider> {
        return listOfNotNull(
            TronNetwork.TronGrid(apiKey = null),
            config.tronGridApiKey.letNotBlank(TronNetwork::TronGrid),
            config.nowNodeCredentials?.apiKey.letNotBlank(TronNetwork::NowNodes),
            config.getBlockCredentials?.tron?.rest.letNotBlank(TronNetwork::GetBlock),
        )
            .map(::TronJsonRpcNetworkProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<TronNetworkProvider> {
        return listOf(TronNetwork.Nile).map(::TronJsonRpcNetworkProvider)
    }
}