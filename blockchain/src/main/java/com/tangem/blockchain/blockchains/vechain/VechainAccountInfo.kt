package com.tangem.blockchain.blockchains.vechain

import com.tangem.blockchain.common.Token
import java.math.BigDecimal

data class VechainAccountInfo(
    val balance: BigDecimal,
    val energy: BigDecimal,
    val completedTxIds: Set<String>,
    val tokenBalances: Map<Token, BigDecimal>,
)