package com.tangem.blockchain.blockchains.algorand.models

import java.math.BigDecimal

internal data class AlgorandAccountModel(
    val coinValue: BigDecimal,
    val reserveValue: BigDecimal,
    val existentialDeposit: BigDecimal,
)