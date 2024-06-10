package com.tangem.blockchain.network.blockbook

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.blockbook.config.GetBlockConfig
import com.tangem.blockchain.network.blockbook.config.NowNodesConfig

internal class BlockBookNetworkProviderFactory(
    private val config: BlockchainSdkConfig,
) {

    fun createNowNodesProvider(blockchain: Blockchain): BitcoinNetworkProvider? {
        return config.nowNodeCredentials?.apiKey?.letNotBlank {
            BlockBookNetworkProvider(
                config = NowNodesConfig(nowNodesCredentials = config.nowNodeCredentials),
                blockchain = blockchain,
            )
        }
    }

    fun createGetBlockProvider(blockchain: Blockchain): BitcoinNetworkProvider? {
        val credentials = config.getBlockCredentials ?: return null
        val accessToken = when (blockchain) {
            Blockchain.Bitcoin -> credentials.bitcoin
            Blockchain.Dash -> credentials.dash
            Blockchain.Dogecoin -> credentials.dogecoin
            Blockchain.Litecoin -> credentials.litecoin
            else -> null
        } ?: return null

        return if (!accessToken.blockBookRest.isNullOrEmpty() && !accessToken.jsonRpc.isNullOrEmpty()) {
            BlockBookNetworkProvider(
                config = GetBlockConfig(
                    blockBookToken = accessToken.blockBookRest,
                    jsonRpcToken = accessToken.jsonRpc,
                ),
                blockchain = blockchain,
            )
        } else {
            null
        }
    }
}