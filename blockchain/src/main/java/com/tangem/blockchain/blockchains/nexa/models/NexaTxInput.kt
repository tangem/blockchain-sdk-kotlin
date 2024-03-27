package com.tangem.blockchain.blockchains.nexa.models


data class NexaTxInput(
    val outpoints: List<NexaTxOutpoint>,
)

/**
 * Nexa UTXO reference
 * @param hash Output hash (hash of the transaction idem and output index)
 */
data class NexaTxOutpoint(
    val hash: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NexaTxOutpoint

        return hash.contentEquals(other.hash)
    }

    override fun hashCode(): Int {
        return hash.contentHashCode()
    }
}