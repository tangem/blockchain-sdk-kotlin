package com.tangem.blockchain.blockchains.sui.model

import java.math.BigDecimal

internal data class SuiCoin(
    val objectId: String,
    val coinType: String,
    val version: Long,
    val digest: String,
    val mistBalance: BigDecimal,
)