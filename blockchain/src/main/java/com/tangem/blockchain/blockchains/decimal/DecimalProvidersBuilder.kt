package com.tangem.blockchain.blockchains.decimal

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class DecimalProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<EthereumJsonRpcProvider>(
    providerTypes,
    listOf("https://testnet-val.decimalchain.com/web3/"),
) {
    override fun createProvider(url: String, blockchain: Blockchain) = EthereumJsonRpcProvider(baseUrl = url)
}