package com.tangem.blockchain.blockchains.ravencoin.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class RavencoinWalletInfoResponse(
    @Json(name = "addrStr") val address: String,

    @Json(name = "balance") val balance: BigDecimal?,
    @Json(name = "balanceSat") val balanceSatoshi: BigDecimal?,

    @Json(name = "totalReceived") val totalReceived: BigDecimal?,
    @Json(name = "totalReceivedSat") val totalReceivedSatoshi: BigDecimal?,

    @Json(name = "totalSent") val totalSent: BigDecimal?,
    @Json(name = "totalSentSat") val totalSentSatoshi: BigDecimal?,

    @Json(name = "unconfirmedBalance") val unconfirmedBalance: BigDecimal,
    @Json(name = "unconfirmedBalanceSat") val unconfirmedBalanceSatoshi: BigDecimal,

    @Json(name = "unconfirmedTxApperances") val unconfirmedTxApperances: Long,
    @Json(name = "txApperances") val txApperances: Long,

    @Json(name = "transactions") val transactions: List<String>,
)

@JsonClass(generateAdapter = true)
data class RavencoinWalletUTXOResponse(
    @Json(name = "txid") val txid: String,
    @Json(name = "vout") val vout: Long,
    @Json(name = "scriptPubKey") val scriptPubKey: String,
    @Json(name = "amount") val amount: BigDecimal,
)

@JsonClass(generateAdapter = true)
data class RavencoinRawTransactionRequest(@Json(name = "rawtx") val rawTx: String)

@JsonClass(generateAdapter = true)
data class RavencoinRawTransactionResponse(@Json(name = "txid") val txId: String)

@JsonClass(generateAdapter = true)
data class RavencoinTransactionHistoryResponse(
    @Json(name = "pagesTotal") val pagesTotal: Long,
    @Json(name = "txs") val transactions: List<RavencoinTransactionInfo>,
)

@JsonClass(generateAdapter = true)
data class RavencoinTransactionInfo(
    @Json(name = "txid") val txid: String,
    @Json(name = "vin") val vin: List<Vin>,
    @Json(name = "vout") val vout: List<Vout>,
    @Json(name = "blockheight") val blockHeight: Long,
    @Json(name = "confirmations") val confirmations: Long,
    @Json(name = "time") val time: Long,
)

@JsonClass(generateAdapter = true)
data class Vin(
    @Json(name = "txid") val txid: String,
    @Json(name = "vout") val vout: Long,
    @Json(name = "scriptSig") val scriptSig: ScriptPubKey?,
    @Json(name = "addr") val address: String,
    @Json(name = "value") val value: BigDecimal,
) {
    @JsonClass(generateAdapter = true)
    data class ScriptPubKey(
        @Json(name = "hex") val hex: String?,
        @Json(name = "asm") val asm: String?,
    )
}

@JsonClass(generateAdapter = true)
data class Vout(
    @Json(name = "value") val value: String,
    @Json(name = "scriptPubKey") val scriptPubKey: ScriptPubKey,
    @Json(name = "spentTxId") val spentTxId: String?,
) {
    @JsonClass(generateAdapter = true)
    data class ScriptPubKey(
        @Json(name = "hex") val hex: String?,
        @Json(name = "asm") val asm: String?,
        @Json(name = "addresses") val addresses: List<String>,
        @Json(name = "type") val type: String?,
    )
}