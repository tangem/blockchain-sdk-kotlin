package com.tangem.blockchain.blockchains.kaspa.krc20.model

import java.math.BigDecimal

internal data class IncompleteTokenTransactionParams(
    val transactionId: String,
    val amountValue: BigDecimal,
    val feeAmountValue: BigDecimal,
    val envelope: Envelope,
)