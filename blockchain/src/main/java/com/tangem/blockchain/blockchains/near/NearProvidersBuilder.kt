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

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Near, Blockchain.NearTestnet)

    override fun createProviders(blockchain: Blockchain): List<NearNetworkProvider> {
        val isTestnet = blockchain.isTestnet()

        return buildList {
            add(createNearJsonRpcProvider(isTestNet = isTestnet))

            if (!isTestnet) {
                config.nowNodeCredentials?.apiKey.letNotBlank(::createNowNodeJsonRpcProvider)?.let(::add)
                config.getBlockCredentials?.near?.jsonRpc.letNotBlank(::createGetBlockJsonRpcProvider)?.let(::add)
            }
        }
    }

    private fun createNearJsonRpcProvider(isTestNet: Boolean): NearNetworkProvider {
        return NearJsonRpcNetworkProvider(
            baseUrl = if (isTestNet) "https://rpc.testnet.near.org/" else "https://rpc.mainnet.near.org/",
        )
    }

    private fun createGetBlockJsonRpcProvider(accessToken: String): NearNetworkProvider {
        return NearJsonRpcNetworkProvider(baseUrl = "https://go.getblock.io/$accessToken/")
    }

    private fun createNowNodeJsonRpcProvider(apiKey: String): NearNetworkProvider {
        return NearJsonRpcNetworkProvider(baseUrl = "https://near.nownodes.io/$apiKey/")
    }
}