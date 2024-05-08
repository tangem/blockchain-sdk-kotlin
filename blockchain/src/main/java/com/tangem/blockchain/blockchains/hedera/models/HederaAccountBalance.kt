package com.tangem.blockchain.blockchains.hedera.models

import java.math.BigDecimal

internal data class HederaAccountBalance(
    val hbarBalance: BigDecimal,
    val tokenBalances: List<TokenBalance>,
) {
    internal data class TokenBalance(val contractAddress: String, val balance: BigDecimal)
}