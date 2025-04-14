package com.tangem.blockchain.blockchains.solana.solanaj.core

import org.p2p.solanaj.core.Account
import org.p2p.solanaj.core.Message
import org.p2p.solanaj.core.PublicKey

/**
[REDACTED_AUTHOR]
 */
internal class SolanaMessage(
    private val feePayerPublicKey: PublicKey,
) : Message() {

    @Deprecated("Override getFeePayerPublicKey instead")
    override fun setFeePayer(feePayer: Account?) {
        throw UnsupportedOperationException()
    }

    override fun getFeePayerPublicKey(): PublicKey = feePayerPublicKey
}