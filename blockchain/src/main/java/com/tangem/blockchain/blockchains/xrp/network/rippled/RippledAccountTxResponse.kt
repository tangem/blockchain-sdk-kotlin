package com.tangem.blockchain.blockchains.xrp.network.rippled

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Response of the `account_tx` rippled method.
 *
 * The API version is intentionally not specified, so the ledger replies with the v1 shape where a transaction is
 * placed into the `tx` field (v2 renamed it to `tx_json`).
 *
 * https://xrpl.org/docs/references/http-websocket-apis/public-api-methods/account-methods/account_tx
 */
@JsonClass(generateAdapter = true)
internal data class RippledAccountTxResponse(
    @Json(name = "result")
    val result: RippledAccountTxResult? = null,
)

@JsonClass(generateAdapter = true)
internal data class RippledAccountTxResult(
    @Json(name = "transactions")
    val transactions: List<RippledTransactionInfo>? = null,

    @Json(name = "marker")
    val marker: RippledMarker? = null,

    @Json(name = "error_code")
    val errorCode: Int? = null,

    @Json(name = "error")
    val error: String? = null,

    @Json(name = "error_message")
    val errorMessage: String? = null,
)

/**
 * Pagination marker. Absent in the response when the last page has been reached.
 *
 * Both fields are optional on purpose: an unexpected marker shape then degrades to "no more pages" instead of
 * failing the whole page parsing.
 */
@JsonClass(generateAdapter = true)
internal data class RippledMarker(
    @Json(name = "ledger")
    val ledger: Long? = null,

    @Json(name = "seq")
    val seq: Long? = null,
)

@JsonClass(generateAdapter = true)
internal data class RippledTransactionInfo(
    @Json(name = "tx")
    val tx: RippledHistoryTransaction,

    @Json(name = "meta")
    val meta: RippledTransactionMeta? = null,

    @Json(name = "validated")
    val isValidated: Boolean? = null,
)

@JsonClass(generateAdapter = true)
internal data class RippledTransactionMeta(
    @Json(name = "TransactionResult")
    val transactionResult: String? = null,
)

@JsonClass(generateAdapter = true)
internal data class RippledHistoryTransaction(
    @Json(name = "Account")
    val account: String,

    @Json(name = "Destination")
    val destination: String? = null,

    @Json(name = "Amount")
    val amount: RippledTransactionAmount? = null,

    /** `TrustSet` operations keep the token data here instead of `Amount` */
    @Json(name = "LimitAmount")
    val limitAmount: RippledIssuedCurrencyAmount? = null,

    /** Amount the `OfferCreate` owner sells */
    @Json(name = "TakerGets")
    val takerGets: RippledTransactionAmount? = null,

    /** Amount the `OfferCreate` owner buys */
    @Json(name = "TakerPays")
    val takerPays: RippledTransactionAmount? = null,

    @Json(name = "Fee")
    val fee: String? = null,

    @Json(name = "TransactionType")
    val transactionType: String? = null,

    @Json(name = "hash")
    val hash: String? = null,

    /** Seconds since the Ripple Epoch */
    @Json(name = "date")
    val date: Long? = null,
)

/**
 * `Amount` is polymorphic: a plain string of drops for XRP and an object for an issued currency.
 *
 * https://xrpl.org/docs/references/protocol/data-types/basic-data-types#specifying-currency-amounts
 */
internal sealed interface RippledTransactionAmount {

    data class Drops(val value: String) : RippledTransactionAmount

    data class IssuedCurrency(val amount: RippledIssuedCurrencyAmount) : RippledTransactionAmount
}

@JsonClass(generateAdapter = true)
internal data class RippledIssuedCurrencyAmount(
    @Json(name = "currency")
    val currency: String,

    @Json(name = "issuer")
    val issuer: String,

    @Json(name = "value")
    val value: String,
)

internal object RippledTransactionAmountAdapter : JsonAdapter<RippledTransactionAmount>() {

    private const val CURRENCY = "currency"
    private const val ISSUER = "issuer"
    private const val VALUE = "value"

    override fun fromJson(reader: JsonReader): RippledTransactionAmount? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> reader.nextNull()
            JsonReader.Token.STRING -> RippledTransactionAmount.Drops(reader.nextString())
            JsonReader.Token.BEGIN_OBJECT -> readIssuedCurrency(reader)
            // Unknown amount shape, e.g. an MPT amount. Such transactions are skipped by the history mapper
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    override fun toJson(writer: JsonWriter, value: RippledTransactionAmount?) {
        when (value) {
            null -> writer.nullValue()
            is RippledTransactionAmount.Drops -> writer.value(value.value)
            is RippledTransactionAmount.IssuedCurrency -> {
                writer.beginObject()
                    .name(CURRENCY).value(value.amount.currency)
                    .name(ISSUER).value(value.amount.issuer)
                    .name(VALUE).value(value.amount.value)
                    .endObject()
            }
        }
    }

    private fun readIssuedCurrency(reader: JsonReader): RippledTransactionAmount? {
        var currency: String? = null
        var issuer: String? = null
        var value: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                CURRENCY -> currency = reader.nextString()
                ISSUER -> issuer = reader.nextString()
                VALUE -> value = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        if (currency == null || issuer == null || value == null) return null

        return RippledTransactionAmount.IssuedCurrency(
            amount = RippledIssuedCurrencyAmount(currency = currency, issuer = issuer, value = value),
        )
    }
}