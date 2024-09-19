package com.tangem.blockchain.blockchains.sui.model

import java.math.BigDecimal

internal data class SuiWalletInfo(
    val totalBalance: BigDecimal,
    val suiCoins: List<SuiCoin>,
)