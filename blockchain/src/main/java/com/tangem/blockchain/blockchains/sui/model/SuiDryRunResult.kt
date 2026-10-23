package com.tangem.blockchain.blockchains.sui.model

import java.math.BigDecimal

internal data class SuiDryRunResult(
    val isSuccess: Boolean,
    val computationCost: BigDecimal,
    val storageCost: BigDecimal,
    val gasPrice: Long,
)