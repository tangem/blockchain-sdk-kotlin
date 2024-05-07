package com.tangem.blockchain.blockchains.hedera.models

internal data class HederaAccountInfo(
    val balance: HederaAccountBalance,
    val pendingTxsInfo: List<HederaTransactionInfo>,
)