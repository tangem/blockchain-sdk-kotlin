package com.tangem.blockchain.blockchains.hedera.models

internal data class HederaTransactionInfo(
    val isPending: Boolean,
    val id: HederaTransactionId,
)