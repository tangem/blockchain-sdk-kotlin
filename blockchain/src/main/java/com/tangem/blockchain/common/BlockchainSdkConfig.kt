package com.tangem.blockchain.common

data class BlockchainSdkConfig (
    val blockchairAuthorizationToken: String? = null,
    val blockcypherTokens: Set<String>? = null,
    val infuraProjectId: String? = null
)