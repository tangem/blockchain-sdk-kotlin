package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.crypto.hdWallet.DerivationPath

/**
 * Information about a single input hash to be signed in a multi-address transaction.
 * Each UTXO input may have a different derived public key and derivation path.
 */
data class MultiAddressSignatureInfo(
    val hash: ByteArray,
    val publicKey: ByteArray,
    val derivationPath: DerivationPath,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MultiAddressSignatureInfo
        return hash.contentEquals(other.hash) &&
            publicKey.contentEquals(other.publicKey) &&
            derivationPath == other.derivationPath
    }

    override fun hashCode(): Int {
        var result = hash.contentHashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + derivationPath.hashCode()
        return result
    }
}