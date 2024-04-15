package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class PulseChainProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<EthereumJsonRpcProvider>(
    providerTypes = providerTypes,
    testnetProviders = listOf(
        "https://rpc.v4.testnet.pulsechain.com/",
        "https://pulsechain-testnet.publicnode.com/",
        "https://rpc-testnet-pulsechain.g4mm4.io/",
    ),
) {

    override fun createProvider(url: String, blockchain: Blockchain) = EthereumJsonRpcProvider(url)
}