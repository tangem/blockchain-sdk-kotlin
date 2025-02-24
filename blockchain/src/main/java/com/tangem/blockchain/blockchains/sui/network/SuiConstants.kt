package com.tangem.blockchain.blockchains.sui.network

import java.math.BigDecimal

internal object SuiConstants {

    const val COIN_TYPE = "0x2::sui::SUI"
    val SUI_GAS_BUDGET_MAX_VALUE = BigDecimal(50_000_000_000)
}