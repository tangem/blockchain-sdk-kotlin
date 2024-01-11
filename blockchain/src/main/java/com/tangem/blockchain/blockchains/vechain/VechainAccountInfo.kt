package com.tangem.blockchain.blockchains.vechain

import java.math.BigDecimal

data class VechainAccountInfo(
    val balance: BigDecimal,
    val energy: BigDecimal,
    val completedTxIds: Set<String>
)