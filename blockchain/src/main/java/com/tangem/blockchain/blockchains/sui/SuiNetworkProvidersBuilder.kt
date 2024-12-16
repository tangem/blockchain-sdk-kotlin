package com.tangem.blockchain.blockchains.sui

import com.tangem.blockchain.blockchains.sui.network.rpc.SuiJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class SuiNetworkProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<SuiJsonRpcProvider>() {

    override fun createProviders(blockchain: Blockchain): List<SuiJsonRpcProvider> {
        return providerTypes.mapNotNull { type ->
            when (type) {
                is ProviderType.Public -> SuiJsonRpcProvider(type.url)
                is ProviderType.GetBlock -> createGetBlockProvider()
                else -> null
            }
        }
    }

    private fun createGetBlockProvider(): SuiJsonRpcProvider? {
        return config.getBlockCredentials?.sui?.jsonRpc.letNotBlank { jsonRpcToken ->
            SuiJsonRpcProvider(baseUrl = "https://go.getblock.io/$jsonRpcToken/")
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<SuiJsonRpcProvider> {
        return listOf(
            SuiJsonRpcProvider(baseUrl = "https://fullnode.testnet.sui.io/"),
        )
    }
}