package com.tangem.blockchain.network.blockbook.config

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.NowNodeCredentials

sealed class BlockBookConfig(val credentials: BlockBookCredentials?) {

    abstract val baseHost: String

    abstract fun getHost(blockchain: Blockchain, request: BlockBookRequest): String

    abstract fun path(request: BlockBookRequest): String

    abstract fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String

    class NowNodes(nowNodesCredentials: NowNodeCredentials) : BlockBookConfig(
        credentials = NowNodeCredentials.headerApiKey to nowNodesCredentials.apiKey
    ) {
        override val baseHost: String = "nownodes.io"

        override fun getHost(blockchain: Blockchain, request: BlockBookRequest): String {
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

                Blockchain.Arbitrum -> "https://arb-blockbook.$baseHost"
                Blockchain.BSC -> "https://bsc-blockbook.$baseHost"
                Blockchain.EthereumTestnet -> "https://${prefix}book-goerli.${baseHost}"
                Blockchain.Polygon -> "https://${prefix}book.$baseHost"
                Blockchain.Kava -> "https://kava-tendermint.$baseHost"
                Blockchain.Ethereum,
                Blockchain.EthereumClassic,
                Blockchain.Avalanche,
                Blockchain.EthereumPow,
                Blockchain.Tron,
                -> "https://${prefix}-blockbook.${baseHost}"

                else -> error("BlockBookConfig.NowNodes don't support blockchain $blockchain")
            }
        }

        override fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String {
            return "${getHost(blockchain, request)}${path(request)}"
        }

        override fun path(request: BlockBookRequest): String = when (request) {
            BlockBookRequest.GetFee -> ""
            else -> "/api/v2"
        }
    }

    class GetBlock(
        private val blockBookToken: String,
        private val jsonRpcToken: String,
    ) : BlockBookConfig(credentials = null) {
        override val baseHost: String = "https://go.getblock.io"

        override fun getHost(blockchain: Blockchain, request: BlockBookRequest): String {
            return when (request) {
                BlockBookRequest.GetFee -> "$baseHost/$jsonRpcToken"
                else -> "$baseHost/$blockBookToken"
            }
        }

        override fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String {
            return "${getHost(blockchain, request)}${path(request)}"
        }

        override fun path(request: BlockBookRequest): String = when (request) {
            BlockBookRequest.GetFee -> "/"
            else -> "/api/v2"
        }
    }
}

typealias BlockBookCredentials = Pair<String, String>