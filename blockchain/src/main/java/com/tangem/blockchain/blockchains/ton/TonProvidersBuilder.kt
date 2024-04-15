package com.tangem.blockchain.blockchains.ton

import com.tangem.blockchain.blockchains.ton.network.TonJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.ton.network.TonNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class TonProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<TonNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<TonNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> createTonCenterJsonRpcProvider(url = it.url)
                ProviderType.NowNodes -> createNowNodeJsonRpcProvider()
                ProviderType.GetBlock -> createGetBlockJsonRpcProvider()
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<TonNetworkProvider> {
        return listOfNotNull(
            createTonCenterJsonRpcProvider(url = "https://testnet.toncenter.com/api/v2/"),
        )
    }

    private fun createTonCenterJsonRpcProvider(url: String): TonJsonRpcNetworkProvider? {
        return config.tonCenterCredentials?.getApiKey().letNotBlank {
            TonJsonRpcNetworkProvider(
                baseUrl = url,
                headerInterceptors = listOf(AddHeaderInterceptor(mapOf("x-api-key" to it))),
            )
        }
    }

    private fun createNowNodeJsonRpcProvider(): TonJsonRpcNetworkProvider? {
        return config.nowNodeCredentials?.apiKey.letNotBlank {
            TonJsonRpcNetworkProvider(baseUrl = "https://ton.nownodes.io/$it/")
        }
    }

    private fun createGetBlockJsonRpcProvider(): TonJsonRpcNetworkProvider? {
        return config.getBlockCredentials?.ton?.jsonRpc.letNotBlank {
            TonJsonRpcNetworkProvider(baseUrl = "https://go.getblock.io/$it/")
        }
    }
}