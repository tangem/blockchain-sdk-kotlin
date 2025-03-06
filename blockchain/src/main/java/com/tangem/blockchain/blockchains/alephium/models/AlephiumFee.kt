package com.tangem.blockchain.blockchains.alephium.models

import java.math.BigDecimal

data class AlephiumFee(
    val gasPrice: BigDecimal,
    val gasAmount: BigDecimal,
    val unsignedTx: String,
)