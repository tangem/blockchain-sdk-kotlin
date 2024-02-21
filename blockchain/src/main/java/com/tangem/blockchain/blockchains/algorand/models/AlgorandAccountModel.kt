package com.tangem.blockchain.blockchains.algorand.models

import java.math.BigDecimal

internal data class AlgorandAccountModel(
    val availableCoinBalance: BigDecimal,
    val reserveValue: BigDecimal,
    val balanceIncludingReserve: BigDecimal,
    val transactionsInfo: List<AlgorandTransactionInfo>,
)