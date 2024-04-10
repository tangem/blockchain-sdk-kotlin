package com.tangem.blockchain.blockchains.koinos.models

internal class KoinosTransactionEntry(
    val id: String,
    val payerAddress: String,
    val maxPayerRC: Long,
    val rcLimit: Long,
    val rcUsed: Long,
    val transferEvent: KoinTransferEvent,
) {

    data class KoinTransferEvent(
        val fromAddress: String,
        val toAddress: String,
        val value: Long,
    )
}