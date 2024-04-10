package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.extensions.letNotBlank

internal class AvalancheProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            // postfix is required because the AVALANCHE API needs url without a last slash !!!
            EthereumJsonRpcProvider(baseUrl = "https://api.avax.network/", postfixUrl = AVALANCHE_POSTFIX),
            config.nowNodeCredentials?.apiKey.letNotBlank { nowNodesApiKey ->
                EthereumJsonRpcProvider(
                    baseUrl = "https://avax.nownodes.io/",
                    postfixUrl = AVALANCHE_POSTFIX,
                    nowNodesApiKey = nowNodesApiKey, // special for Avalanche
                )
            },
            config.getBlockCredentials?.avalanche?.jsonRpc.letNotBlank { avalancheToken ->
                EthereumJsonRpcProvider(
                    baseUrl = "https://go.getblock.io/$avalancheToken/",
                    postfixUrl = AVALANCHE_POSTFIX,
                )
            },
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider(
                baseUrl = "https://api.avax-test.network/",
                postfixUrl = AVALANCHE_POSTFIX,
            ),
        )
    }

    private companion object {
        const val AVALANCHE_POSTFIX = "ext/bc/C/rpc"
    }
}