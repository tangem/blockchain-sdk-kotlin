package com.tangem.blockchain.blockchains.ton.models

import java.math.BigDecimal

internal data class TonWalletInfo(
    val balance: BigDecimal,
    val sequenceNumber: Int,
)