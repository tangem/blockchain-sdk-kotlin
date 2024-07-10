package com.tangem.blockchain.blockchains.tron

import com.tangem.blockchain.common.TransactionExtras

class TronTransactionExtras(
    val data: ByteArray,
    val txType: TransactionType,
) : TransactionExtras

enum class TransactionType {
    APPROVE,
}