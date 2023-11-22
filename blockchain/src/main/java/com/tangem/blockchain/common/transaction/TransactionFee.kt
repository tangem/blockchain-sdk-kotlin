package com.tangem.blockchain.common.transaction

sealed class TransactionFee {

    abstract val normal: Fee

    data class Choosable(
        override val normal: Fee,
        val minimum: Fee,
        val priority: Fee,
    ) : TransactionFee()

    data class Single(
        override val normal: Fee,
    ) : TransactionFee()
}
