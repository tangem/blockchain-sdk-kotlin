package com.tangem.blockchain.network.blockbook.network.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.EnumeratedEnum

@JsonClass(generateAdapter = true)
data class GetAddressResponse(
    @Json(name = "balance") val balance: String,
    @Json(name = "unconfirmedTxs") val unconfirmedTxs: Int?,
    @Json(name = "txs") val txs: Int,
    @Json(name = "transactions") val transactions: List<Transaction>?,
    @Json(name = "page") val page: Int?,
    @Json(name = "totalPages") val totalPages: Int?,
    @Json(name = "itemsOnPage") val itemsOnPage: Int?,
    @Json(name = "tokens") val trxTokens: List<TrxToken>?,
) {

    @JsonClass(generateAdapter = true)
    data class TrxToken(
        @Json(name = "type") val type: String?,
        @Json(name = "name") val name: String?,
        @Json(name = "id") val id: String?,
        @Json(name = "transfers") val transfers: Int?,
        @Json(name = "balance") val balance: String?,
    )

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
        // ** TRX specific fields **//
        @Json(name = "tronTXReceipt") val tronTXReceipt: TronTXReceipt?,
        @Json(name = "fromAddress") val fromAddress: String?,
        @Json(name = "toAddress") val toAddress: String?,
        @Json(name = "contract_type") val contractType: Int?,
        @Json(name = "contract_name") val contractAddress: String?,
    ) {

        /**
         * Tron blockchain specific info.
         * There are many more fields in this response, but we map only the required ones.
         */
        data class TronTXReceipt(val status: StatusType?)

        enum class StatusType(override val value: Int) : EnumeratedEnum {
            PENDING(-1),
            FAILURE(0),
            OK(1),
            ;
        }

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
            @Json(name = "contract") val contract: String?,
            @Json(name = "token") val token: String?,
            @Json(name = "name") val name: String?,
            @Json(name = "symbol") val symbol: String?,
            @Json(name = "value") val value: String?,
        )

        @JsonClass(generateAdapter = true)
        data class EthereumSpecific(
            @Json(name = "status") val status: StatusType?,
            @Json(name = "nonce") val nonce: Int?,
            @Json(name = "gasLimit") val gasLimit: Long?,
            @Json(name = "gasUsed") val gasUsed: Long?,
            @Json(name = "gasPrice") val gasPrice: String?,
            @Json(name = "data") val data: String?,
            @Json(name = "parsedData") val parsedData: ParsedData?,
        ) {

            @JsonClass(generateAdapter = true)
            data class ParsedData(
                // / First 4byte from data. E.g. `0x617ba037`
                @Json(name = "methodId") val methodId: String,
                @Json(name = "name") val name: String,
            )
        }
    }
}