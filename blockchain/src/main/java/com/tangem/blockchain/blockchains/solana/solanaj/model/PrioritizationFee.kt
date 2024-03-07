package com.tangem.blockchain.blockchains.solana.solanaj.model

internal data class PrioritizationFee(
    val slot: Long,
    val prioritizationFee: Long,
)