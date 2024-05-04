package com.tangem.blockchain.blockchains.koinos.models

import java.math.BigDecimal

internal data class KoinosAccountInfo(
    val koinBalance: BigDecimal,
    val mana: BigDecimal,
)