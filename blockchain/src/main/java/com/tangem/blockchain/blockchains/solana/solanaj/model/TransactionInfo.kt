package com.tangem.blockchain.blockchains.solana.solanaj.model

import org.p2p.solanaj.rpc.types.TransactionResult

internal data class TransactionInfo(
    val signature: String,
    val fee: Long, // in lamports
    val instructions: List<TransactionResult.Instruction>,
)
