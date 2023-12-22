package com.tangem.blockchain.network.blockchair

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Token

@JsonClass(generateAdapter = true)
data class BlockchairAddress(
    val data: Map<String, BlockchairAddressData>? = null,
)

data class BlockchairAddressData(
    @Json(name = "address")
    val addressInfo: BlockchairAddressInfo? = null,

    @Json(name = "utxo")
    val unspentOutputs: List<BlockchairUnspentOutput>? = null, // btc-like only

    val transactions: List<BlockchairTransactionInfo>? = null, // btc-like only

    val calls: List<BlockchairCallInfo>? = null, // eth-like only
)

data class BlockchairAddressInfo(
    val balance: Long? = null,

    @Json(name = "script_hex")
    val script: String? = null,

    @Json(name = "output_count")
    val outputCount: Int? = null,

    @Json(name = "unspent_output_count")
    val unspentOutputCount: Int? = null,
)

data class BlockchairUnspentOutput(
    @Json(name = "block_id")
    val block: Int? = null,

    @Json(name = "transaction_hash")
    val transactionHash: String? = null,

    val index: Int? = null,

    @Json(name = "value")
    val amount: Long? = null,
)

data class BlockchairTransaction(
    val data: Map<String, BlockchairTransactionData>? = null,
)

data class BlockchairTransactionData(
    val transaction: BlockchairTransactionInfo? = null,
)

data class BlockchairTransactionInfo(
    @Json(name = "block_id")
    val block: Int? = null,

    val hash: String? = null,

    val time: String? = null,

    @Json(name = "balance_change")
    val balanceDif: Long? = null,
)

data class BlockchairStats(
    val data: BlockchairStatsData? = null,
)

data class BlockchairStatsData(
    @Json(name = "suggested_transaction_fee_per_byte_sat")
    val feePerByte: Int? = null,
)

data class BlockchairTokenHolder(
    val data: Map<String, TokenHolderData>,
)

data class TokenHolderData(
    val transactions: List<BlockchairCallInfo>? = null,
)

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
    val contractAddress: String? = null,
)

@JsonClass(generateAdapter = true)
data class BlockchairTokensResponse(
    val data: Map<String, BlockchairTokensData>? = null,
)

data class BlockchairTokensData(
    @Json(name = "layer_2")
    val tokensInfo: TokensInfo,
)

data class TokensInfo(
    @Json(name = "erc_20")
    val tokens: List<BlockchairToken>,
)

// data class Erc20Tokens(val tokens: List<BlockchairToken>)
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
