package com.tangem.blockchain.blockchains.sui.network

import java.math.BigDecimal

internal object SuiConstants {

    const val COIN_TYPE = "0x2::sui::SUI"
    val SUI_GAS_BUDGET_MAX_VALUE = BigDecimal(50_000_000_000)

    private const val COIN_TYPE_LONG = "0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI"

    /**
     * Extracts `T` from a `0x2::coin::Coin<T>` object type. gRPC pads every address to 32 bytes, while the SDK keys
     * the native coin by the short [COIN_TYPE]; token types are padded by the wallet manager anyway.
     */
    fun coinTypeOf(coinObjectType: String): String {
        val coinType = coinObjectType.substringAfter(delimiter = "<").removeSuffix(suffix = ">")
        return if (coinType == COIN_TYPE_LONG) COIN_TYPE else coinType
    }
}