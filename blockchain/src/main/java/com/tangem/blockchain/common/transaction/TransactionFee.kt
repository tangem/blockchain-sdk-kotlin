package com.tangem.blockchain.common.transaction

import com.tangem.blockchain.common.Amount

sealed class TransactionFee {

    data class SetOfThree(
        val minFee: Amount,
        val normalFee: Amount,
        val priorityFee: Amount,
    ) : TransactionFee()

    data class Single(
        val fee: Amount
    ) : TransactionFee()

}