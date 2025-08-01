package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.extensions.letNotBlank

internal class EthereumJsonRpcProviderFactory(
    private val config: BlockchainSdkConfig,
) {

    fun getPublicProvider(baseUrl: String): EthereumJsonRpcProvider {
        return EthereumJsonRpcProvider(baseUrl = baseUrl)
    }

    fun getNowNodesProvider(baseUrl: String): EthereumJsonRpcProvider? {
        return config.nowNodeCredentials?.apiKey.letNotBlank { nowNodesApiKey ->
            EthereumJsonRpcProvider(baseUrl = baseUrl, postfixUrl = nowNodesApiKey)
        }
    }

    fun getGetBlockProvider(accessToken: GetBlockCredentials.() -> String?): EthereumJsonRpcProvider? {
        return config.getBlockCredentials?.accessToken()?.letNotBlank {
            EthereumJsonRpcProvider(baseUrl = "https://go.getblock.io/$it/")
        }
    }

    fun getInfuraProvider(baseUrl: String): EthereumJsonRpcProvider? {
        return config.infuraProjectId.letNotBlank { infuraProjectId ->
            EthereumJsonRpcProvider(baseUrl = baseUrl, postfixUrl = infuraProjectId)
        }
    }
}