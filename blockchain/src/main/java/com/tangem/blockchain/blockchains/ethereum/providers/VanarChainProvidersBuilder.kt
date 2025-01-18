package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class VanarChainProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : NetworkProvidersBuilder<EthereumJsonRpcProvider>() {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider("https://rpc.vanarchain.com"), // FIXME: use config during full integration
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider("https://rpc-vanguard.vanarchain.com"),
        )
    }
}