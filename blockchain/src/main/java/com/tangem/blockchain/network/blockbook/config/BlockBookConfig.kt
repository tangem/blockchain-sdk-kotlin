package com.tangem.blockchain.network.blockbook.config

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.common.NowNodeCredentials

sealed class BlockBookConfig(val credentials: BlockBookCredentials) {

    abstract val host: String

    abstract fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String

    class NowNodes(nowNodesCredentials: NowNodeCredentials) : BlockBookConfig(
        credentials = NowNodeCredentials.headerApiKey to nowNodesCredentials.apiKey
    ) {
        override val host: String = "nownodes.io"

        override fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String {
            val currencySymbolPrefix = blockchain.currency.lowercase()
            return when (request) {
                BlockBookRequest.GET_FEE -> "https://$currencySymbolPrefix.$host"
                else -> {
                    val testnetSuffix = if (blockchain.isTestnet()) "-testnet" else ""
                    return "https://${currencySymbolPrefix}book$testnetSuffix.$host/api/v2"
                }
            }
        }
    }

    class GetBlock(getBlockCredentials: GetBlockCredentials) : BlockBookConfig(
        credentials = BlockchainSdkConfig.X_API_KEY_HEADER to getBlockCredentials.apiKey
    ) {
        override val host: String = "getblock.io"

        override fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String {
            val currencySymbolPrefix = blockchain.currency.lowercase()
            return when (request) {
                BlockBookRequest.GET_FEE -> "https://$currencySymbolPrefix.$host/mainnet"
                else -> "https://$currencySymbolPrefix.$host/mainnet/blockbook/api/v2"
            }
        }
    }
}

typealias BlockBookCredentials = Pair<String, String>