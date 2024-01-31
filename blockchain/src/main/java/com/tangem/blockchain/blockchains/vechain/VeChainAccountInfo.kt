package com.tangem.blockchain.blockchains.vechain

import com.tangem.blockchain.common.Token
import java.math.BigDecimal

data class VeChainAccountInfo(
    val balance: BigDecimal,
    val energy: BigDecimal,
    val completedTxIds: Set<String>,
    val tokenBalances: Map<Token, BigDecimal>,
)
