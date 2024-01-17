package com.tangem.blockchain.blockchains.aptos

import com.tangem.blockchain.blockchains.aptos.network.AptosNetworkProvider
import com.tangem.blockchain.blockchains.aptos.network.provider.AptosJsonRpcNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.extensions.letNotBlank

internal class AptosNetworkProvidersBuilder(
    private val blockchain: Blockchain,
    private val config: BlockchainSdkConfig,
) {

    fun build(): List<AptosNetworkProvider> {
        return listOfNotNull(
            createOfficialNetworkProvider(),
            config.getBlockCredentials?.aptos?.rest?.letNotBlank(::createGetBlockNetworkProvider),
            config.nowNodeCredentials?.apiKey?.letNotBlank(::createNowNodesNetworkProvider),
        )
    }

    private fun createOfficialNetworkProvider(): AptosNetworkProvider {
        return AptosJsonRpcNetworkProvider(
            baseUrl = "https://fullnode.${if (blockchain.isTestnet()) "testnet" else "mainnet"}.aptoslabs.com/",
        )
    }

    private fun createNowNodesNetworkProvider(apiKey: String): AptosNetworkProvider {
        return AptosJsonRpcNetworkProvider(baseUrl = "https://apt.nownodes.io/$apiKey/")
    }

    private fun createGetBlockNetworkProvider(accessToken: String): AptosJsonRpcNetworkProvider {
        return AptosJsonRpcNetworkProvider(baseUrl = "https://go.getblock.io/$accessToken/")
    }
}