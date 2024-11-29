package com.tangem.blockchain.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Locale

@JsonClass(generateAdapter = true)
data class Token(
    @Json(name = "name")
    val name: String,
    @Json(name = "symbol")
    val symbol: String,
    @Json(name = "contractAddress")
    val contractAddress: String,
    @Json(name = "decimals")
    val decimals: Int,
    @Json(name = "id")
    val id: String? = null,
) {
    constructor(
        symbol: String,
        contractAddress: String,
        decimals: Int,
    ) : this(symbol, symbol, contractAddress, decimals)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Token

        if (symbol != other.symbol) return false
        if (contractAddress.lowercase(Locale.ROOT) != other.contractAddress.lowercase(Locale.ROOT)) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = symbol.hashCode()
        result = 31 * result + contractAddress.lowercase(Locale.ROOT).hashCode()
        return result
    }
}