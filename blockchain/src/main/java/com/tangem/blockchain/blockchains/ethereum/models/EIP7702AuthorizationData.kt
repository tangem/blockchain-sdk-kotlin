package com.tangem.blockchain.blockchains.ethereum.models

import java.math.BigInteger

data class EIP7702AuthorizationData(
    val chainId: Int,
    val nonce: BigInteger,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EIP7702AuthorizationData

        if (chainId != other.chainId) return false
        if (nonce != other.nonce) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chainId
        result = 31 * result + nonce.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}