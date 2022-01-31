package com.tangem.blockchain.blockchains.solana.solanaj.core

import org.bitcoinj.core.Base58
import org.p2p.solanaj.core.Account
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.Transaction

/**
[REDACTED_AUTHOR]
 */
class Transaction(
    private val feePayerPublicKey: PublicKey
) : Transaction(Message(feePayerPublicKey)) {

    override fun sign(signer: Account?) {
        throw UnsupportedOperationException()
    }

    override fun sign(signers: List<Account>) {
        throw UnsupportedOperationException()
    }

    fun getDataForSign(): ByteArray {
        serializedMessage = message.serialize()
        return serializedMessage
    }

    fun addSignedDataSignature(signedSignature: ByteArray) {
        signatures.add(Base58.encode(signedSignature))
    }
}