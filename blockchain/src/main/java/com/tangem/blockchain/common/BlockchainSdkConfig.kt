package com.tangem.blockchain.common

data class BlockchainSdkConfig(
    val blockchairCredentials: BlockchairCredentials? = null,
    val blockcypherTokens: Set<String>? = null,
    val quickNodeBscCredentials: QuickNodeBscCredentials? = null,
    val infuraProjectId: String? = null,
    val tronGridApiKey: String? = null,
)

data class BlockchairCredentials(
    val apiKey: List<String>,
    val authToken: String,
)

data class QuickNodeBscCredentials(
    val apiKey: String,
    val subdomain: String,
)