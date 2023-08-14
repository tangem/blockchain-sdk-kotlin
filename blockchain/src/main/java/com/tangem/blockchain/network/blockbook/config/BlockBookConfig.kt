package com.tangem.blockchain.network.blockbook.config

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.common.NowNodeCredentials

sealed class BlockBookConfig(val credentials: BlockBookCredentials) {

    abstract val baseHost: String

    fun getHost(blockchain: Blockchain): String {
        return "https://${blockchain.currency.lowercase()}.$baseHost"
    }

    abstract fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String

    class NowNodes(nowNodesCredentials: NowNodeCredentials) : BlockBookConfig(
        credentials = NowNodeCredentials.headerApiKey to nowNodesCredentials.apiKey
    ) {
        override val baseHost: String = "nownodes.io"

        override fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String {

            return when (request) {
                BlockBookRequest.GET_FEE -> getHost(blockchain)
                else -> {
                    val currencySymbolPrefix = blockchain.currency.lowercase()
                    val testnetSuffix = if (blockchain.isTestnet()) "-testnet" else ""
                    return "https://${currencySymbolPrefix}book$testnetSuffix.$baseHost/api/v2"
                }
            }
        }
    }

    class GetBlock(getBlockCredentials: GetBlockCredentials) : BlockBookConfig(
        credentials = GetBlockCredentials.HEADER_PARAM_NAME to getBlockCredentials.apiKey
    ) {
        override val baseHost: String = "getblock.io"

        override fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String {
            return when (request) {
                BlockBookRequest.GET_FEE -> "{${getHost(blockchain)}}/mainnet"
                else -> {
                    val currencySymbolPrefix = blockchain.currency.lowercase()
                    "https://$currencySymbolPrefix.$baseHost/mainnet/blockbook/api/v2"
                }
            }
        }
    }
}

typealias BlockBookCredentials = Pair<String, String>
