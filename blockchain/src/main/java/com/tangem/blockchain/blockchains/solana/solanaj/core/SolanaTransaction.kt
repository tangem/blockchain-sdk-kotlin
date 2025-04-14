package com.tangem.blockchain.blockchains.solana.solanaj.core

import org.bitcoinj.core.Base58
import org.p2p.solanaj.core.Account
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.Transaction

/**
[REDACTED_AUTHOR]
 */
class SolanaTransaction(
    feePayerPublicKey: PublicKey,
) : Transaction(SolanaMessage(feePayerPublicKey)) {

    @Deprecated("Instead, use getDataForSign and then addSignedDataSignature before submitting the transaction.")
    override fun sign(signer: Account?) {
        throw UnsupportedOperationException()
    }

    @Deprecated("Instead, use getDataForSign and then addSignedDataSignature before submitting the transaction.")
    override fun sign(signers: List<Account>) {
        throw UnsupportedOperationException()
    }

    fun setSerializedMessage(serializedMessage: ByteArray) {
        this.serializedMessage = serializedMessage
    }

    fun getSerializedMessage(): ByteArray {
        serializedMessage = message.serialize()
        return serializedMessage
    }

    fun addSignedDataSignature(signedSignature: ByteArray) {
        signatures.add(Base58.encode(signedSignature))
    }
}