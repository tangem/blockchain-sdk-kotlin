package com.tangem.blockchain.tokenbalance.models

import java.math.BigDecimal

data class TokenBalance(
    val contractAddress: String?,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val amount: BigDecimal,
    val isNativeToken: Boolean,
)