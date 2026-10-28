package com.tangem.blockchain.blockchains.sui.model

import java.math.BigDecimal

internal data class SuiBalance(
    val coinType: String,
    val mistAddressBalance: BigDecimal,
)