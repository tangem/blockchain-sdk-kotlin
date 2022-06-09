package com.tangem.blockchain.common

data class BlockchainSdkConfig (
    val blockchairApiKey: String? = null,
    val blockchairAuthorizationToken: String? = null,
    val blockcypherTokens: Set<String>? = null,
    val infuraProjectId: String? = null,
    val infuraArbitrumProjectId: String? = null
)