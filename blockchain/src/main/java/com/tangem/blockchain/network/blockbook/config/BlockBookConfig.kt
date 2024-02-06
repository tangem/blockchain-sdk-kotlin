package com.tangem.blockchain.network.blockbook.config

import com.tangem.blockchain.common.Blockchain

typealias BlockBookCredentials = Pair<String, String>

sealed class BlockBookConfig(val credentials: BlockBookCredentials?) {

    abstract val baseHost: String

    fun getRequestBaseUrl(request: BlockBookRequest, blockchain: Blockchain): String {
        return "${getHost(blockchain, request)}${path(request)}"
    }

    protected abstract fun getHost(blockchain: Blockchain, request: BlockBookRequest): String

    protected abstract fun path(request: BlockBookRequest): String
}
