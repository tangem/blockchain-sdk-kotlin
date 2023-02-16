package com.tangem.blockchain.network.blockbook.config

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.common.NowNodeCredentials

sealed class BlockBookConfig(val credentials: BlockBookCredentials) {

    abstract fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String

    class NowNodes(nowNodesCredentials: NowNodeCredentials) : BlockBookConfig(
        credentials = NowNodeCredentials.headerApiKey to nowNodesCredentials.apiKey
    ) {

        override fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String {
            val currencySymbolPrefix = blockchain.currency.lowercase()
            return when (request) {
                BlockBookRequest.GET_FEE -> "https://$currencySymbolPrefix.nownodes.io"
                else -> {
                    val testnetSuffix = if (blockchain.isTestnet()) "-testnet" else ""
                    return "https://${currencySymbolPrefix}book$testnetSuffix.nownodes.io/api/v2"
                }
            }
        }
    }

    class GetBlock(getBlockCredentials: GetBlockCredentials) : BlockBookConfig(
        credentials = GetBlockCredentials.headerApiKey to getBlockCredentials.apiKey
    ) {

        override fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String {
            val currencySymbolPrefix = blockchain.currency.lowercase()
            return when (request) {
                BlockBookRequest.GET_FEE -> "https://$currencySymbolPrefix.getblock.io/mainnet"
                else -> "https://$currencySymbolPrefix.getblock.io/mainnet/blockbook/api/v2"
            }
        }
    }
}

typealias BlockBookCredentials = Pair<String, String>