package com.tangem.blockchain.network.blockbook.config

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.common.NowNodeCredentials

sealed class BlockBookConfig(val credentials: BlockBookCredentials) {

    abstract val baseHost: String

    abstract fun getHost(blockchain: Blockchain): String

     abstract fun path(request: BlockBookRequest): String

    abstract fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String

    class NowNodes(nowNodesCredentials: NowNodeCredentials) : BlockBookConfig(
        credentials = NowNodeCredentials.headerApiKey to nowNodesCredentials.apiKey
    ) {
        override val baseHost: String = "nownodes.io"

        override fun getHost(blockchain: Blockchain): String {
            val prefix = blockchain.currency.lowercase()
            return when (blockchain) {
                Blockchain.Bitcoin, Blockchain.BitcoinTestnet,
                Blockchain.Dash,
                Blockchain.Litecoin,
                Blockchain.Dogecoin,
                -> {
                    val testnetSuffix = if (blockchain.isTestnet()) "-testnet" else ""
                    "https://${prefix}book${testnetSuffix}.${baseHost}"
                }

                Blockchain.Ethereum -> "https://${prefix}-blockbook.${baseHost}"
                else -> error("BlockBookConfig.NowNodes don't support blockchain $blockchain")
            }
        }

        override fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String {
            return "${getHost(blockchain)}${path(request)}"
        }

        override fun path(request: BlockBookRequest): String = when(request) {
            BlockBookRequest.GetFee -> ""
            else -> "/api/v2"
        }
    }

    class GetBlock(getBlockCredentials: GetBlockCredentials) : BlockBookConfig(
        credentials = GetBlockCredentials.HEADER_PARAM_NAME to getBlockCredentials.apiKey
    ) {
        override val baseHost: String = "getblock.io"

        override fun getHost(blockchain: Blockchain): String {
            val currencySymbolPrefix = blockchain.currency.lowercase()
            return "https://$currencySymbolPrefix.$baseHost"
        }

        override fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String {
            return "${getHost(blockchain)}${path(request)}"
        }

        override fun path(request: BlockBookRequest): String = when(request) {
            BlockBookRequest.GetFee -> "/mainnet"
            else -> "/mainnet/blockbook/api/v2"
        }
    }
}

typealias BlockBookCredentials = Pair<String, String>
