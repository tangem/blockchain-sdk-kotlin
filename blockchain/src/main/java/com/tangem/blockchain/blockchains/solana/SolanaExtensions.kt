package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import org.p2p.solanaj.rpc.types.TokenAccountInfo
import org.p2p.solanaj.rpc.types.config.Commitment
import java.math.BigDecimal
import java.math.RoundingMode

/**
[REDACTED_AUTHOR]
 */
internal fun Long.toSOL(): BigDecimal = this.toBigDecimal().toSOL()
internal fun BigDecimal.toSOL(): BigDecimal = movePointLeft(Blockchain.Solana.decimals()).toSolanaDecimals()
internal fun BigDecimal.toLamports(): Long = movePointRight(Blockchain.Solana.decimals()).toSolanaDecimals().toLong()
internal fun BigDecimal.toSolanaDecimals(): BigDecimal = this.setScale(Blockchain.Solana.decimals(), RoundingMode.HALF_UP)

internal fun Commitment.toMap(): MutableMap<String, Any> {
    return mutableMapOf("commitment" to this)
}

internal fun MutableMap<String, Any>.addCommitment(commitment: Commitment): MutableMap<String, Any> {
    this["commitment"] = commitment
    return this
}

internal fun List<TokenAccountInfo.Value>.retrieveLamportsBy(token: Token): Long? {
    return getSplTokenBy(token)?.account?.lamports
}

internal fun List<TokenAccountInfo.Value>.getSplTokenBy(token: Token): TokenAccountInfo.Value? {
    return firstOrNull { it.pubkey == token.contractAddress }
}