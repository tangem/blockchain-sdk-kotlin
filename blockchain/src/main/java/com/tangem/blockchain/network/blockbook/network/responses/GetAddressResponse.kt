package com.tangem.blockchain.network.blockbook.network.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetAddressResponse(
    @Json(name = "balance") val balance: String,
    @Json(name = "unconfirmedTxs") val unconfirmedTxs: Int,
    @Json(name = "txs") val txs: Int,
    @Json(name = "transactions") val transactions: List<Transaction>?,
    @Json(name = "page") val page: Int?,
    @Json(name = "totalPages") val totalPages: Int?,
    @Json(name = "itemsOnPage") val itemsOnPage: Int?,
) {

    @JsonClass(generateAdapter = true)
    data class Transaction(
        @Json(name = "txid") val txid: String,
        @Json(name = "vout") val vout: List<Vout> = emptyList(),
        @Json(name = "confirmations") val confirmations: Int,
        @Json(name = "blockTime") val blockTime: Int,
        @Json(name = "value") val value: String,
        @Json(name = "vin") val vin: List<Vin> = emptyList(),
        @Json(name = "fees") val fees: String,
        @Json(name = "tokenTransfers") val tokenTransfers: List<TokenTransfer> = emptyList(),
        @Json(name = "ethereumSpecific") val ethereumSpecific: EthereumSpecific? = null,
    ) {

        @JsonClass(generateAdapter = true)
        data class Vin(
            @Json(name = "addresses") val addresses: List<String>?,
            @Json(name = "value") val value: String?,
        )

        @JsonClass(generateAdapter = true)
        data class Vout(
            @Json(name = "addresses") val addresses: List<String>?,
            @Json(name = "hex") val hex: String?,
            @Json(name = "value") val value: String?,
        )

        @JsonClass(generateAdapter = true)
        data class TokenTransfer(
            @Json(name = "type") val type: String?,
            @Json(name = "from") val from: String,
            @Json(name = "to") val to: String,
            @Json(name = "contract") val contract: String,
            @Json(name = "name") val name: String?,
            @Json(name = "symbol") val symbol: String?,
            @Json(name = "decimals") val decimals: Int,
            @Json(name = "value") val value: String?,
        )

        @JsonClass(generateAdapter = true)
        data class EthereumSpecific(
            @Json(name = "status") val status: Int?,
            @Json(name = "nonce") val nonce: Int?,
            @Json(name = "gasLimit") val gasLimit: Long?,
            @Json(name = "gasUsed") val gasUsed: Long?,
            @Json(name = "gasPrice") val gasPrice: String?,
            @Json(name = "data") val data: String?,
            @Json(name = "parsedData") val parsedData: ParsedData?,
        ) {

            enum class StatusType(val type: Int) {
                PENDING(-1),
                FAILURE(0),
                OK(1),
                ;

                companion object {
                    fun fromType(type: Int): StatusType {
                        return values().firstOrNull { it.type == type } ?: error("StatusType for $type is not found.")
                    }
                }
            }

            @JsonClass(generateAdapter = true)
            data class ParsedData(
                /// First 4byte from data. E.g. `0x617ba037`
                @Json(name = "methodId") val methodId: String,
                @Json(name = "name") val name: String,
            )
        }
    }
}
