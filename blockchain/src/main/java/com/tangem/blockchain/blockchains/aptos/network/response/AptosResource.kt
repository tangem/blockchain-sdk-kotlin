package com.tangem.blockchain.blockchains.aptos.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/** Aptos account resources */
internal sealed class AptosResource {

    data class AccountResource(val sequenceNumber: String) : AptosResource()

    data class CoinResource(val balance: BigDecimal) : AptosResource()

    data class TokenResource(val contractAddress: String, val balance: BigDecimal) : AptosResource()

    data class Unknown(val type: String) : AptosResource()
}

@JsonClass(generateAdapter = true)
data class AptosAccountDataBody(
    @Json(name = "sequence_number") val sequenceNumber: String,
)

@JsonClass(generateAdapter = true)
data class AptosCoinDataBody(
    @Json(name = "coin") val coin: Coin,
) {

    @JsonClass(generateAdapter = true)
    data class Coin(
        @Json(name = "value") val value: String,
    )
}
