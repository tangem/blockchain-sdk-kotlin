package com.tangem.blockchain.blockchains.filecoin.network.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Filecoin transaction body
 *
 * @property sourceAddress      source address
 * @property destinationAddress destination address
 * @property amount             amount
 * @property gasUnitPrice       gas unit price
 * @property gasLimit           gas limit
 * @property gasPremium         gas premium
 * @constructor Create empty Filecoin transaction body
 */
@JsonClass(generateAdapter = true)
internal data class FilecoinTransactionBody(
    @Json(name = "From") val sourceAddress: String,
    @Json(name = "To") val destinationAddress: String,
    @Json(name = "Value") val amount: String,
    @Json(name = "Nonce") val nonce: Long?,
    @Json(name = "GasFeeCap") val gasUnitPrice: String?,
    @Json(name = "GasLimit") val gasLimit: Long?,
    @Json(name = "GasPremium") val gasPremium: String?,
)