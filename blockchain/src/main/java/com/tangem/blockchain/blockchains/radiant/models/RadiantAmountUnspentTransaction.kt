package com.tangem.blockchain.blockchains.radiant.models

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee

internal data class RadiantAmountUnspentTransaction(
    val amount: Amount,
    val fee: Fee?,
    val unspents: List<RadiantUnspentTransaction>,
) {
    val amountSatoshiDecimalValue = amount.longValue ?: 0L
    val changeSatoshiDecimalValue get() = calculateChange(
        unspents = unspents,
        amountSatoshi = amountSatoshiDecimalValue,
        feeSatoshi = feeSatoshiDecimalValue,
    )
    private val feeSatoshiDecimalValue = fee?.amount?.longValue ?: 0L

    private fun calculateChange(
        unspents: List<RadiantUnspentTransaction>,
        amountSatoshi: Long,
        feeSatoshi: Long,
    ): Long {
        val fullAmountSatoshi = unspents.sumOf { it.amount }
        return fullAmountSatoshi - amountSatoshi - feeSatoshi
    }
}