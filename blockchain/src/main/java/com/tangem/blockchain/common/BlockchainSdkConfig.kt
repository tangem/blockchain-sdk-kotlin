package com.tangem.blockchain.common

data class BlockchainSdkConfig(
    val blockchairCredentials: BlockchairCredentials? = null,
    val blockcypherTokens: Set<String>? = null,
    val quickNodeBscCredentials: QuickNodeCredentials? = null,
    val quickNodeSolanaCredentials: QuickNodeCredentials? = null,
    val nowNodeCredentials: NowNodeCredentials? = null,
    val getBlockCredentials: GetBlockCredentials? = null,
    val tonCenterCredentials: TonCenterCredentials? = null,
    val infuraProjectId: String? = null,
    val tronGridApiKey: String? = null,
    val kaspaSecondaryApiUrl: String? = null,
    val chiaFireAcademyApiKey: String? = null,
    val chiaTangemApiKey: String? = null,
)

data class BlockchairCredentials(
    val apiKey: List<String>,
    val authToken: String?,
)

data class BlockscoutCredentials(
    val userName: String,
    val password: String,
)

data class QuickNodeCredentials(val apiKey: String, val subdomain: String)

data class NowNodeCredentials(
    val apiKey: String,
) {
    companion object {
        const val headerApiKey = "api-key"
    }
}

data class GetBlockCredentials(
    val apiKey: String,
) {
    companion object {
        const val HEADER_PARAM_NAME = "x-api-key"
        const val QUERY_PARAM_NAME = "api_key"
    }
}

data class TonCenterCredentials(
    private val mainnetApiKey: String,
    private val testnetApiKey: String,
) {
    fun getApiKey(testnet: Boolean = false): String {
        return if (testnet) testnetApiKey else mainnetApiKey
    }
}
