package com.tangem.blockchain.tokenbalance.models

import java.math.BigDecimal

data class TokenBalance(
    val contractAddress: String?,
    val amount: BigDecimal,
    val isNativeToken: Boolean,
)