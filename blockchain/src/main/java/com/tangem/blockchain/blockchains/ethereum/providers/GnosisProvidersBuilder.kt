package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class GnosisProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            ethereumProviderFactory.getGetBlockProvider { gnosis?.jsonRpc },
            EthereumJsonRpcProvider(baseUrl = "https://rpc.gnosischain.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://gnosis-mainnet.public.blastapi.io/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ankr.com/gnosis/"),
        )
    }
}