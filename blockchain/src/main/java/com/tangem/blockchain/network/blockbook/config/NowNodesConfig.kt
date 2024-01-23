package com.tangem.blockchain.network.blockbook.config

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.NowNodeCredentials

internal class NowNodesConfig(nowNodesCredentials: NowNodeCredentials) : BlockBookConfig(
    credentials = NowNodeCredentials.headerApiKey to nowNodesCredentials.apiKey,
) {
    override val baseHost: String = "nownodes.io"

    override fun getHost(blockchain: Blockchain, request: BlockBookRequest): String {
        val isRpcRequest = request.isRpcRequest()
        val prefix = blockchain.currency.lowercase()

        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet,
            Blockchain.Dash,
            Blockchain.Litecoin,
            Blockchain.Dogecoin,
            -> {
                if (isRpcRequest) {
                    "https://$prefix.$baseHost"
                } else {
                    val testnetSuffix = if (blockchain.isTestnet()) "-testnet" else ""
                    "https://${prefix}book$testnetSuffix.$baseHost"
                }
            }
            Blockchain.Arbitrum -> {
                if (isRpcRequest) {
                    // L2 blockchains use `currencySymbol` from their L1s, so we can't just
                    // use the `prefix` variable here for L2s like Arbitrum, Optimism, etc
                    "https://arbitrum.$baseHost"
                } else {
                    "https://arb-blockbook.$baseHost"
                }
            }
            Blockchain.BSC -> {
                if (isRpcRequest) {
                    "https://bsc.$baseHost"
                } else {
                    "https://bsc-blockbook.$baseHost"
                }
            }
            Blockchain.EthereumTestnet -> {
                "https://${prefix}book-goerli.$baseHost"
            }
            Blockchain.Polygon -> {
                "https://${prefix}book.$baseHost"
            }
            Blockchain.Kava -> {
                "https://kava-tendermint.$baseHost"
            }
            Blockchain.Ethereum,
            Blockchain.EthereumClassic,
            Blockchain.Avalanche,
            Blockchain.EthereumPow,
            Blockchain.Tron,
            -> {
                if (isRpcRequest) {
                    "https://$prefix.$baseHost"
                } else {
                    "https://$prefix-blockbook.$baseHost"
                }
            }
            else -> {
                error("BlockBookConfig.NowNodes don't support blockchain $blockchain")
            }
        }
    }

    override fun path(request: BlockBookRequest): String = when (request) {
        BlockBookRequest.GetFee -> ""
        else -> "/api/v2"
    }

    private fun BlockBookRequest.isRpcRequest() = this is BlockBookRequest.GetFee
}
