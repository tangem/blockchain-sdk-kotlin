package com.tangem.blockchain.blockchains.sui.model

import java.math.BigDecimal

internal data class SuiEpoch(
    val epoch: Long,
    val referenceGasPrice: BigDecimal,
)