package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class AreonProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<EthereumJsonRpcProvider>(
    providerTypes = providerTypes,
    testnetProviders = listOf(
        "https://testnet-rpc.areon.network/",
        "https://testnet-rpc2.areon.network/",
        "https://testnet-rpc3.areon.network/",
        "https://testnet-rpc4.areon.network/",
        "https://testnet-rpc5.areon.network/",
    ),
) {

    override fun createProvider(url: String, blockchain: Blockchain) = EthereumJsonRpcProvider(url)
}