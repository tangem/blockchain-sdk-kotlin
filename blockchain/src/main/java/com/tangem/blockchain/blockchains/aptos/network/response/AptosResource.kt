package com.tangem.blockchain.blockchains.aptos.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

internal sealed class AptosResource {

    @JsonClass(generateAdapter = true)
    data class AccountResource(
        @Json(name = "data") val account: AccountData,
    ) : AptosResource() {

        @JsonClass(generateAdapter = true)
        data class AccountData(
            @Json(name = "sequence_number") val sequenceNumber: String,
        )
    }

    @JsonClass(generateAdapter = true)
    data class CoinResource(
        @Json(name = "data") val coin: CoinData,
    ) : AptosResource() {

        @JsonClass(generateAdapter = true)
        data class CoinData(
            @Json(name = "coin") val coin: Coin,
        ) {

            @JsonClass(generateAdapter = true)
            data class Coin(
                @Json(name = "value") val value: String,
            )
        }
    }

    object Unknown : AptosResource()

    companion object {

        fun createPolymorphicJsonAdapterFactory(): PolymorphicJsonAdapterFactory<AptosResource> {
            return PolymorphicJsonAdapterFactory
                .of(AptosResource::class.java, "type")
                .withSubtype(AccountResource::class.java, "0x1::account::Account")
                .withSubtype(CoinResource::class.java, "0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>")
                .withDefaultValue(Unknown)
        }
    }
}