package com.tangem.blockchain.network.blockchair

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Token

@JsonClass(generateAdapter = true)
data class BlockchairAddress(
        val data: Map<String, BlockchairAddressData>? = null
)

@JsonClass(generateAdapter = true)
data class BlockchairAddressData(
        @Json(name = "address")
        val addressInfo: BlockchairAddressInfo? = null,

        @Json(name = "utxo")
        val unspentOutputs: List<BlockchairUnspentOutput>? = null, //btc-like only

        val transactions: List<BlockchairTransactionInfo>? = null, //btc-like only

        val calls: List<BlockchairCallInfo>? = null //eth-like only
)

@JsonClass(generateAdapter = true)
data class BlockchairAddressInfo(
        val balance: Long? = null,

        @Json(name = "script_hex")
        val script: String? = null,

        @Json(name = "output_count")
        val outputCount: Int? = null,

        @Json(name = "unspent_output_count")
        val unspentOutputCount: Int? = null // only in address data
)

@JsonClass(generateAdapter = true)
data class BlockchairUnspentOutput(
        @Json(name = "block_id")
        val block: Int? = null,

        @Json(name = "transaction_hash")
        val transactionHash: String? = null,

        val index: Int? = null,

        val value: Long? = null
)

@JsonClass(generateAdapter = true)
data class BlockchairTransaction(
        val data: Map<String, BlockchairTransactionData>? = null
)

@JsonClass(generateAdapter = true)
data class BlockchairTransactionData(
        val transaction: BlockchairTransactionInfo? = null,
        val inputs: List<BlockchairInput>? = null,
        val outputs: List<BlockchairOutput>? = null
)

@JsonClass(generateAdapter = true)
data class BlockchairTransactionInfo(
        @Json(name = "block_id")
        val block: Int? = null,

        val hash: String? = null,

        val time: String? = null,

        @Json(name = "balance_change")
        val balanceDif: Long? = null
)

@JsonClass(generateAdapter = true)
data class BlockchairStats(
        val data: BlockchairStatsData? = null
)

@JsonClass(generateAdapter = true)
data class BlockchairStatsData(
        @Json(name = "suggested_transaction_fee_per_byte_sat")
        val feePerByte: Int? = null
)

@JsonClass(generateAdapter = true)
data class BlockchairTokenHolder(
        val data: Map<String, TokenHolderData>
)

@JsonClass(generateAdapter = true)
data class TokenHolderData(
        val transactions: List<BlockchairCallInfo>? = null
)

@JsonClass(generateAdapter = true)
data class BlockchairCallInfo(
        @Json(name = "block_id")
        val block: Int? = null,

        @Json(name = "transaction_hash")
        val hash: String? = null,

        val time: String? = null,

        val sender: String? = null,

        val recipient: String? = null,

        val value: String? = null,

        @Json(name = "token_symbol")
        val tokenSymbol: String? = null,

        @Json(name = "token_decimals")
        val tokenDecimals: Int? = null,

        @Json(name = "token_address")
        val contractAddress: String? = null
)

@JsonClass(generateAdapter = true)
data class BlockchairTokensResponse(
        val data: Map<String, BlockchairTokensData>? = null
)

@JsonClass(generateAdapter = true)
data class BlockchairTokensData(
        @Json(name = "layer_2")
        val tokensInfo: TokensInfo
)

@JsonClass(generateAdapter = true)
data class TokensInfo(
        @Json(name = "erc_20")
        val tokens: List<BlockchairToken>
)

//data class Erc20Tokens(val tokens: List<BlockchairToken>)
@JsonClass(generateAdapter = true)
data class BlockchairToken(
        @Json(name = "token_address")
        val address: String,

        @Json(name = "token_name")
        val name: String,

        @Json(name = "token_symbol")
        val symbol: String,

        @Json(name = "token_decimals")
        val decimals: Int,

        @Json(name = "balance_approximate")
        val approximateBalance: Double,

        val balance: String,
) {
    fun toToken(): Token {
        return Token(name = name, symbol = symbol, contractAddress = address, decimals = decimals)
    }
}

@JsonClass(generateAdapter = true)
data class BlockchairInput(
        @Json(name = "block_id")
        val block: Int? = null,

        @Json(name = "transaction_hash")
        val transactionHash: String? = null,

        val index: Int? = null,

        val value: Long? = null,

        @Json(name = "script_hex")
        val script: String? = null,

        @Json(name = "spending_sequence")
        val sequence: Long? = null,

        val recipient: String? = null
)

@JsonClass(generateAdapter = true)
data class BlockchairOutput(
        val value: Long? = null,

        val recipient: String? = null
)