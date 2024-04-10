package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class CronosProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://evm.cronos.org/"),
            EthereumJsonRpcProvider(baseUrl = "https://evm-cronos.crypto.org/"),
            ethereumProviderFactory.getGetBlockProvider { cronos?.jsonRpc },
            EthereumJsonRpcProvider(baseUrl = "https://cronos.blockpi.network/v1/rpc/public/"),
            EthereumJsonRpcProvider(baseUrl = "https://cronos-evm.publicnode.com/"),
        )
    }
}