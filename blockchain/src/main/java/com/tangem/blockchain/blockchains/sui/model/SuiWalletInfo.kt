package com.tangem.blockchain.blockchains.sui.model

import java.math.BigDecimal

internal data class SuiWalletInfo(
    val suiTotalBalance: BigDecimal,
    val coins: List<SuiCoin>,
)