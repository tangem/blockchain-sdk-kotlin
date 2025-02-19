package com.tangem.blockchain.blockchains.sui.network

import java.math.BigDecimal

internal object SuiConstants {

    const val COIN_TYPE = "0x2::sui::SUI"
    const val MIST_SCALE = 9
    val suiGasBudgetMaxValue = BigDecimal(50_000_000_000)
}