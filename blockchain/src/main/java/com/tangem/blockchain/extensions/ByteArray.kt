package com.tangem.blockchain.extensions

import android.util.Base64
import com.tangem.blockchain.blockchains.cardano.crypto.Blake2b
import com.tangem.blockchain.blockchains.tron.libs.Base58Check
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ECKey
import org.spongycastle.crypto.util.DigestFactory
import java.math.BigInteger

fun ByteArray.encodeBase58(checked: Boolean = false): String {
    return if (checked) Base58Check.bytesToBase58(this) else Base58.encode(this)
}

fun ByteArray.encodeBase64NoWrap(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}

fun ByteArray.encodeBase64(): String {
    return Base64.encodeToString(this, Base64.DEFAULT)
}

@Suppress("MagicNumber")
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

@Suppress("MagicNumber")
fun ByteArray.toCanonicalECDSASignature(): ECKey.ECDSASignature {
    if (this.size != 64) error("Invalid signature length")
    val r = BigInteger(1, this.copyOfRange(0, 32))
    val s = BigInteger(1, this.copyOfRange(32, 64))
    return ECKey.ECDSASignature(r, s).toCanonicalised()
}
