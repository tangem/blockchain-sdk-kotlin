package com.tangem.blockchain.blockchains.near

import com.tangem.blockchain.blockchains.near.network.NearJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.near.network.NearNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank

internal class NearProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<NearNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<NearNetworkProvider> {
        return listOfNotNull(
            createNearJsonRpcProvider(isTestnet = false),
            createNowNodeJsonRpcProvider(),
            createGetBlockJsonRpcProvider(),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<NearNetworkProvider> {
        return listOf(
            createNearJsonRpcProvider(isTestnet = true),
        )
    }

    private fun createNearJsonRpcProvider(isTestnet: Boolean): NearNetworkProvider {
        return NearJsonRpcNetworkProvider(
            baseUrl = if (isTestnet) "https://rpc.testnet.near.org/" else "https://rpc.mainnet.near.org/",
        )
    }

    private fun createNowNodeJsonRpcProvider(): NearNetworkProvider? {
        return config.nowNodeCredentials?.apiKey.letNotBlank {
            NearJsonRpcNetworkProvider(baseUrl = "https://near.nownodes.io/$it/")
        }
    }

    private fun createGetBlockJsonRpcProvider(): NearNetworkProvider? {
        return config.getBlockCredentials?.near?.jsonRpc.letNotBlank {
            NearJsonRpcNetworkProvider(baseUrl = "https://go.getblock.io/$it/")
        }
    }
}