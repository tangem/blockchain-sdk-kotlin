package com.tangem.blockchain.blockchains.ton

import com.tangem.blockchain.blockchains.ton.network.TonJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.ton.network.TonNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank

internal class TonProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<TonNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.TON, Blockchain.TONTestnet)

    override fun createProviders(blockchain: Blockchain): List<TonNetworkProvider> {
        return buildList {
            val isTestnet = blockchain.isTestnet()

            config.tonCenterCredentials?.getApiKey().letNotBlank {
                add(createTonCenterJsonRpcProvider(isTestNet = isTestnet, apiKey = it))
            }

            if (!isTestnet) {
                config.nowNodeCredentials?.apiKey.letNotBlank {
                    add(createNowNodeJsonRpcProvider(it))
                }

                config.getBlockCredentials?.ton?.jsonRpc.letNotBlank {
                    add(createGetBlockJsonRpcProvider(it))
                }
            }
        }
    }

    private fun createTonCenterJsonRpcProvider(isTestNet: Boolean, apiKey: String): TonJsonRpcNetworkProvider {
        val url = if (isTestNet) "https://testnet.toncenter.com/api/v2/" else "https://toncenter.com/api/v2/"

        return TonJsonRpcNetworkProvider(
            baseUrl = url,
            headerInterceptors = listOf(AddHeaderInterceptor(mapOf("x-api-key" to apiKey))),
        )
    }

    private fun createGetBlockJsonRpcProvider(accessToken: String): TonJsonRpcNetworkProvider {
        return TonJsonRpcNetworkProvider(baseUrl = "https://go.getblock.io/$accessToken/")
    }

    private fun createNowNodeJsonRpcProvider(apiKey: String): TonJsonRpcNetworkProvider {
        return TonJsonRpcNetworkProvider(baseUrl = "https://ton.nownodes.io/$apiKey/")
    }
}