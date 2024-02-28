package com.tangem.blockchain.blockchains.ton

import com.tangem.blockchain.blockchains.ton.network.TonApi
import com.tangem.blockchain.blockchains.ton.network.TonJsonRpcNetworkProvider
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.createRetrofitInstance

class TonJsonRpcClientBuilder {

    fun build(isTestNet: Boolean, blockchainSdkConfig: BlockchainSdkConfig): List<TonJsonRpcNetworkProvider> {
        return buildList {
            blockchainSdkConfig.tonCenterCredentials?.getApiKey(isTestNet).letNotBlank {
                add(createTonCenterJsonRpcProvider(isTestNet = isTestNet, apiKey = it))
            }

            if (!isTestNet) {
                blockchainSdkConfig.nowNodeCredentials?.apiKey.letNotBlank { add(createNowNodeJsonRpcProvider(it)) }
                blockchainSdkConfig.getBlockCredentials?.ton?.jsonRpc.letNotBlank {
                    add(createGetBlockJsonRpcProvider(it))
                }
            }
        }
    }

    private fun createTonCenterJsonRpcProvider(isTestNet: Boolean, apiKey: String): TonJsonRpcNetworkProvider {
        val url = if (isTestNet) "https://testnet.toncenter.com/api/v2/" else "https://toncenter.com/api/v2/"
        val tonApi = createRetrofitInstance(
            baseUrl = url,
            headerInterceptors = listOf(AddHeaderInterceptor(mapOf("x-api-key" to apiKey))),
        ).create(TonApi::class.java)
        return TonJsonRpcNetworkProvider(baseUrl = url, api = tonApi)
    }

    private fun createGetBlockJsonRpcProvider(accessToken: String): TonJsonRpcNetworkProvider {
        val url = "https://go.getblock.io/$accessToken/"
        val tonApi = createRetrofitInstance(url).create(TonApi::class.java)
        return TonJsonRpcNetworkProvider(baseUrl = url, api = tonApi)
    }

    private fun createNowNodeJsonRpcProvider(apiKey: String): TonJsonRpcNetworkProvider {
        val url = "https://ton.nownodes.io/"
        val tonApi = createRetrofitInstance("$url$apiKey/").create(TonApi::class.java)
        return TonJsonRpcNetworkProvider(baseUrl = url, api = tonApi)
    }
}
