package com.tangem.blockchain.network.electrum

import java.math.BigDecimal

data class ElectrumAccount(
    val confirmedAmount: BigDecimal,
    val unconfirmedAmount: BigDecimal,
)

data class ElectrumUnspentUTXORecord(
    val height: Long,
    val txPos: Long,
    val txHash: String,
    val value: BigDecimal,
    val outpointHash: String?, // TODO is Nexa epecific?
) {
    val isConfirmed: Boolean = height != 0L
}

@JvmInline
value class ElectrumEstimateFee(
    // null if the daemon does not have enough information to make an estimate
    val feeInCoinsPer1000Bytes: BigDecimal?,
)