package com.tangem.blockchain.blockchains.solana.solanaj.model

import org.p2p.solanaj.core.PublicKey
import java.math.BigDecimal

internal data class SolanaMainAccountInfo(
    val value: NewSolanaAccountInfo.Value?,
    val tokensByMint: Map<String, SolanaTokenAccountInfo>,
    val txsInProgress: List<TransactionInfo>,
) {
    val balance: Long
        get() = value?.lamports ?: 0
}

internal data class SolanaSplAccountInfo(
    val value: NewSolanaTokenResultObjects.Value,
    val associatedPubK: PublicKey,
)

internal data class SolanaTokenAccountInfo(
    val value: NewSolanaTokenAccountInfo.Value,
    val address: String,
    val mint: String,
    val solAmount: BigDecimal,
)