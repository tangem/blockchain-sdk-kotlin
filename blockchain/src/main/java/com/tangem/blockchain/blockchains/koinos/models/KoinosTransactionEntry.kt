package com.tangem.blockchain.blockchains.koinos.models

internal data class KoinosTransactionEntry(
    val id: String,
    val sequenceNum: Long,
    val payerAddress: String,
    val maxPayerRC: Long,
    val rcLimit: Long,
    val rcUsed: Long,
    val event: Event,
) {

    sealed interface Event {
        data class KoinTransferEvent(
            val fromAddress: Address,
            val toAddress: Address,
            val value: Long,
        ) : Event

        object Unsupported : Event
    }

    sealed interface Address {
        @JvmInline
        value class Single(val address: String) : Address

        @JvmInline
        value class Multiple(val addresses: List<String>) : Address
    }
}