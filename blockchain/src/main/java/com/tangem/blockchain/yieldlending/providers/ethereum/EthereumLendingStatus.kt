package com.tangem.blockchain.yieldlending.providers.ethereum

import java.math.BigDecimal

data class EthereumLendingStatus(
    val isInitialized: Boolean,
    val isActive: Boolean,
    val maxNetworkFee: BigDecimal,
)