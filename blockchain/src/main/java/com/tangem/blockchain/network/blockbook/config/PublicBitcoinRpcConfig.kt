package com.tangem.blockchain.network.blockbook.config

import com.tangem.blockchain.common.Blockchain

internal class PublicBitcoinRpcConfig(override val baseHost: String) : BlockBookConfig(credentials = null) {

    override fun getHost(blockchain: Blockchain, request: BlockBookRequest): String = baseHost

    override fun path(request: BlockBookRequest): String = ""
}