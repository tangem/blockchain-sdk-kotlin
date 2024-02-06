package com.tangem.blockchain.blockchains.solana.solanaj.core

import org.p2p.solanaj.core.Account
import org.p2p.solanaj.core.Message
import org.p2p.solanaj.core.PublicKey

/**
 * Created by Anton Zhilenkov on 26/01/2022.
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
