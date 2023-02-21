package com.tangem.blockchain.common

data class BlockchainSdkConfig(
    val blockchairCredentials: BlockchairCredentials? = null,
    val blockcypherTokens: Set<String>? = null,
    val blockscoutCredentials: BlockscoutCredentials? = null,
    val quickNodeBscCredentials: QuickNodeCredentials? = null,
    val quickNodeSolanaCredentials: QuickNodeCredentials? = null,
    val nowNodeCredentials: NowNodeCredentials? = null,
    val getBlockCredentials: GetBlockCredentials? = null,
    val infuraProjectId: String? = null,
    val tronGridApiKey: String? = null,
    val saltPayAuthToken: String? = null
)

data class BlockchairCredentials(
    val apiKey: List<String>,
    val authToken: String?,
)

data class BlockscoutCredentials(
    val userName: String,
    val password: String,
)

data class QuickNodeCredentials(
    val apiKey: String,
    val subdomain: String,
) {
    fun isNotEmpty(): Boolean = apiKey.isNotEmpty() && subdomain.isNotEmpty()
}

data class NowNodeCredentials(
    val apiKey: String,
){
    companion object {
        const val headerApiKey = "api-key"
    }
}

data class GetBlockCredentials(
    val apiKey: String,
) {
    companion object {
        const val headerApiKey = "x-api-key"
    }
}