package com.tangem.blockchain.extensions

import android.util.Base64
import com.tangem.blockchain.blockchains.cardano.crypto.Blake2b
import org.bitcoinj.core.Base58
import org.spongycastle.crypto.util.DigestFactory

fun ByteArray.encodeBase58(): String {
    return Base58.encode(this)
}

fun ByteArray.encodeBase64NoWrap(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}

fun ByteArray.calculateSha3v256(): ByteArray {
    val sha3Digest = DigestFactory.createSHA3_256()
    sha3Digest.update(this, 0, this.size)
    val sha3Hash = ByteArray(32)
    sha3Digest.doFinal(sha3Hash, 0)
    return sha3Hash
}

fun ByteArray.calculateBlake2b(digestByteSize: Int): ByteArray {
    val digest = Blake2b.Digest.newInstance(digestByteSize)
    return digest.digest(this)
}