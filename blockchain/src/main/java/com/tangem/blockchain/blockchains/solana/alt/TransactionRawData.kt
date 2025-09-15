package com.tangem.blockchain.blockchains.solana.alt

import foundation.metaplex.solana.transactions.CompiledInstruction
import foundation.metaplex.solana.transactions.MessageHeader

internal data class TransactionRawData(
    val payer: ByteArray,
    val staticAccountAddresses: List<ByteArray>,
    val writableAltAddresses: List<ByteArray>,
    val readonlyAltAddresses: List<ByteArray>,
    val recentBlockhash: ByteArray,
    val compiledInstructions: List<CompiledInstruction>,
    val messageHeader: MessageHeader,
    val compiledAltTable: List<CompiledAltTable>?,
)

internal data class CompiledAltTable(
    val account: ByteArray,
    val writableIndexes: List<Int>,
    val readonlyIndexes: List<Int>,
)

internal data class AltAddress(
    val address: ByteArray,
    val isWritable: Boolean,
)