package com.tangem.blockchain.common.transaction

import com.tangem.blockchain.common.Amount

sealed class TransactionFee {

    data class Choosable(
        val minimum: Amount,
        val normal: Amount,
        val priority: Amount,
    ) : TransactionFee()

    data class Single(
        val value: Amount
    ) : TransactionFee()

}