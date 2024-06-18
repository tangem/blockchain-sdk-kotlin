package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProviderFactory
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder

internal abstract class EthereumLikeProvidersBuilder(
    protected open val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<EthereumJsonRpcProvider>() {

    protected val ethereumProviderFactory by lazy { EthereumJsonRpcProviderFactory(config) }
}