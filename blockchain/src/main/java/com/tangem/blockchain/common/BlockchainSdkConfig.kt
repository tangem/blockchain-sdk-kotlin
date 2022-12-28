package com.tangem.blockchain.common

data class BlockchainSdkConfig(
    val blockchairCredentials: BlockchairCredentials? = null,
    val blockcypherTokens: Set<String>? = null,
    val bscQuickNodeCredentials: QuickNodeCredentials? = null,
    val quickNodeCredentials: QuickNodeCredentials? = null,
    val infuraProjectId: String? = null,
    val tronGridApiKey: String? = null,
)

data class BlockchairCredentials(
    val apiKey: List<String>,
    val authToken: String?,
)

data class QuickNodeCredentials(
    val apiKey: String,
    val subdomain: String,
)