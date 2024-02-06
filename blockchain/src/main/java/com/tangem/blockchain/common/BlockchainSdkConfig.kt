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

data class QuickNodeCredentials(val apiKey: String, val subdomain: String)

data class NowNodeCredentials(
    val apiKey: String,
) {
    companion object {
        const val headerApiKey = "api-key"
    }
}

data class GetBlockCredentials(
    val xrp: GetBlockAccessToken,
    val cardano: GetBlockAccessToken?,
    val avalanche: GetBlockAccessToken?,
    val eth: GetBlockAccessToken?,
    val etc: GetBlockAccessToken?,
    val fantom: GetBlockAccessToken?,
    val rsk: GetBlockAccessToken?,
    val bsc: GetBlockAccessToken?,
    val polygon: GetBlockAccessToken?,
    val gnosis: GetBlockAccessToken?,
    val cronos: GetBlockAccessToken?,
    val solana: GetBlockAccessToken?,
    val ton: GetBlockAccessToken?,
    val tron: GetBlockAccessToken?,
    val cosmos: GetBlockAccessToken?,
    val near: GetBlockAccessToken?,
    val dogecoin: GetBlockAccessToken,
    val litecoin: GetBlockAccessToken,
    val dash: GetBlockAccessToken,
    val bitcoin: GetBlockAccessToken,
    val aptos: GetBlockAccessToken?,
)

data class GetBlockAccessToken(
    val jsonRpc: String? = null,
    val blockBookRest: String? = null,
    val rest: String? = null,
    val rosetta: String? = null,
)

data class TonCenterCredentials(
    private val mainnetApiKey: String,
    private val testnetApiKey: String,
) {
    fun getApiKey(testnet: Boolean = false): String {
        return if (testnet) testnetApiKey else mainnetApiKey
    }
}
