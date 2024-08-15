package com.tangem.blockchain.blockchains.filecoin.network.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Filecoin signed transaction body
 *
 * @property transactionBody transaction body
 * @property signature       signature
 */
@JsonClass(generateAdapter = true)
internal data class FilecoinSignedTransactionBody(
    @Json(name = "Message") val transactionBody: FilecoinTransactionBody,
    @Json(name = "Signature") val signature: Signature,
) {

    /**
     * Signature
     *
     * @property type      signature type
     * @property signature signature
     */
    @JsonClass(generateAdapter = true)
    data class Signature(
        @Json(name = "Type") val type: Int,
        @Json(name = "Data") val signature: String,
    )
}