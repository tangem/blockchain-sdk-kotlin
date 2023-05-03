package com.tangem.blockchain.blockchains.cosmos

import com.tangem.blockchain.common.Amount

data class CosmosAccountInfo(
    val accountNumber: Long?,
    val sequenceNumber: Long,
    val amount: Amount,
)