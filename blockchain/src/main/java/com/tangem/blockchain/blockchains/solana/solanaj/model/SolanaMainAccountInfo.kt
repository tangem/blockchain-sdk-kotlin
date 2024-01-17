package com.tangem.blockchain.blockchains.solana.solanaj.model

import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.rpc.types.TokenAccountInfo
import org.p2p.solanaj.rpc.types.TokenResultObjects
import java.math.BigDecimal

internal data class SolanaMainAccountInfo(
    val value: SolanaAccountInfo.Value?,
    val tokensByMint: Map<String, SolanaTokenAccountInfo>,
    val txsInProgress: List<TransactionInfo>,
) {
    val balance: Long
        get() = value?.lamports ?: 0
}

internal data class SolanaSplAccountInfo(
    val value: TokenResultObjects.Value,
    val associatedPubK: PublicKey,
)

internal data class SolanaTokenAccountInfo(
    val value: TokenAccountInfo.Value,
    val address: String,
    val mint: String,
    val uiAmount: BigDecimal, // in SOL
)