package com.tangem.blockchain.network.blockbook.config

import com.tangem.blockchain.common.Blockchain

internal class GetBlockConfig(
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

    override fun path(request: BlockBookRequest): String = when (request) {
        BlockBookRequest.GetFee -> "/"
        else -> "/api/v2"
    }
}
