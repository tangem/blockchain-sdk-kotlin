package com.tangem.blockchain.network.blockbook.config

import com.tangem.blockchain.common.Blockchain

internal class DogecoinMockBlockBookConfig : BlockBookConfig(credentials = null) {

    override val baseHost = "[REDACTED_ENV_URL]"

    override fun getHost(blockchain: Blockchain, request: BlockBookRequest): String = baseHost

    override fun path(request: BlockBookRequest): String = "api/v2"
}