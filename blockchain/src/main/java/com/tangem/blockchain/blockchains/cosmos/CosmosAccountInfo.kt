package com.tangem.blockchain.blockchains.cosmos

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Token

data class CosmosAccountInfo(
    val accountNumber: Long?,
    val sequenceNumber: Long,
    val amount: Amount,
    val tokenBalances: Map<Token, Amount>,
    val confirmedTransactionHashes: List<String>,
)