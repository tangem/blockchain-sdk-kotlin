package com.tangem.blockchain.blockchains.radiant

import com.ripple.crypto.ecdsa.ECDSASignature
import com.tangem.blockchain.blockchains.radiant.models.RadiantUnspentTransaction
import com.tangem.blockchain.extensions.bytes4LittleEndian
import com.tangem.blockchain.extensions.bytes8LittleEndian
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import wallet.core.jni.BitcoinScript
import wallet.core.jni.CoinType
import java.io.ByteArrayOutputStream
import java.math.BigInteger

@Suppress("MagicNumber")
internal object RadiantScriptUtils {

    fun buildSignedScripts(signatures: List<ByteArray>, publicKey: ByteArray): List<ByteArray> =
        signatures.map { signature ->
            val signDer = encodeDerSignature(signature)
            with(ByteArrayOutputStream()) {
                write(signDer.size + 1)
                write(signDer)
                write(0x41)
                write(0x21)
                write(publicKey)
                toByteArray()
            }
        }

    fun buildOutputScript(address: String): ByteArray {
        return BitcoinScript.lockScriptForAddress(address, CoinType.BITCOINCASH).data()
    }

    fun generatePrevoutHash(unspents: List<RadiantUnspentTransaction>): ByteArray {
        val prevouts = with(ByteArrayOutputStream()) {
            unspents.forEach {
                write(it.hash.reversedArray() + it.outputIndex.toInt().bytes4LittleEndian())
            }
            toByteArray()
        }

        return Sha256Hash.hashTwice(prevouts)
    }

    fun generateSequenceHash(unspents: List<RadiantUnspentTransaction>): ByteArray {
        val sequence = ByteArray(4 * unspents.size) { 0xFF.toByte() }
        return Sha256Hash.hashTwice(sequence)
    }

    /**
     * Default BitcoinCash implementation for set hash output values transaction data
     */
    fun writeHashOutput(amount: Long, sourceAddress: String, targetAddress: String, change: Long): ByteArray {
        val sendScript = buildOutputScript(address = targetAddress)
        val outputs = with(ByteArrayOutputStream()) {
            write(amount.bytes8LittleEndian())
            write(sendScript.size)
            write(sendScript)
            if (change != 0L) {
                write(change.bytes8LittleEndian())
                val changeOutputScriptBytes = buildOutputScript(address = sourceAddress)
                write(changeOutputScriptBytes.size)
                write(changeOutputScriptBytes)
            }
            toByteArray()
        }
        return Sha256Hash.hashTwice(outputs)
    }

    /**
     * Specify for radiant blockchain. See comment [here](https://github.com/RadiantBlockchain/radiant-node/blob/master/src/primitives/transaction.h#L493) for how it works
     * Since your transactions won't contain pushrefs, it will be very simple, like the commit I sent above
     */
    fun writeHashOutputHashes(amount: Long, sourceAddress: String, targetAddress: String, change: Long): ByteArray {
        val zeroRef = ByteArray(32) { 0 }

        val outputs = with(ByteArrayOutputStream()) {
            write(amount.bytes8LittleEndian())

            val sendScript = buildOutputScript(address = targetAddress)
            // Hash of the locking script
            val scriptHash = Sha256Hash.hashTwice(sendScript)

            write(scriptHash)
            write(0.bytes4LittleEndian())
            write(zeroRef)

            if (change != 0L) {
                write(change.bytes8LittleEndian())
                val changeOutputScriptBytes = buildOutputScript(address = sourceAddress)
                val changeScriptHash = Sha256Hash.hashTwice(changeOutputScriptBytes)
                write(changeScriptHash)
                write(0.bytes4LittleEndian())
                write(zeroRef)
            }
            toByteArray()
        }

        return Sha256Hash.hashTwice(outputs)
    }

    private fun encodeDerSignature(signature: ByteArray): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
        val ecdsaSignature = ECDSASignature(r, canonicalS)
        return ecdsaSignature.encodeToDER()
    }
}