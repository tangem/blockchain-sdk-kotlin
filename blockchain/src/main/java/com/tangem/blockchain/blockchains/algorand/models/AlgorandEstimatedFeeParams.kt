package com.tangem.blockchain.blockchains.algorand.models

import java.math.BigDecimal

internal data class AlgorandEstimatedFeeParams(
    val minFee: BigDecimal,
    val fee: BigDecimal,
)