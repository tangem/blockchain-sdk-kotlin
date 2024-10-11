package com.tangem.blockchain.blockchains.casper.cashaddr

import org.bouncycastle.jcajce.provider.digest.Blake2b

/**
 * @see <a href="https://github.com/casper-ecosystem/casper-js-sdk/blob/dev/src/lib/ChecksummedHex.ts">Source</a>
 */
object CasperAddressUtils {
    // Ed25519: encode([0x01]) + encode(<public key bytes>)
    // or
    // Secp256k1: encode([0x02]) + encode(<public key bytes>)
    fun ByteArray.checksum(): String = encode(byteArrayOf(first())) + encode(drop(1).toByteArray())

    // Separate bytes inside ByteArray to nibbles
    // E.g. [0x01, 0x55, 0xFF, ...] -> [0x00, 0x01, 0x50, 0x05, 0xF0, 0x0F, ...]
    @Suppress("MagicNumber")
    private fun bytesToNibbles(bytes: ByteArray): ByteArray = bytes
        .flatMap { byte ->
            listOf(
                (byte.toInt() and 0xff shr 4).toByte(),
                (byte.toInt() and 0x0f).toByte(),
            )
        }.toByteArray()

    // Separate bytes inside ByteArray to bits array
    // E.g. [0x01, ...] -> [false, false, false, false, false, false, false, true, ...]
    // E.g. [0xAA, ...] -> [true, false, true, false, true, false, true, false, ...]
    @Suppress("MagicNumber")
    private fun ByteArray.toBitArray(): BooleanArray = this
        .flatMap { byte ->
            List(8) { i ->
                byte.toInt() shr i and 0x01 == 0x01
            }
        }.toBooleanArray()

    private fun byteHash(bytes: ByteArray): ByteArray = Blake2b.Blake2b256().digest(bytes)

    private fun encode(input: ByteArray): String {
        val inputNibbles = bytesToNibbles(input)
        val hash = byteHash(input)
        val hashBits = hash.toBitArray()
        val hashBitsValues = hashBits.iterator()
        return inputNibbles.fold(StringBuilder()) { accum, nibble ->
            val c = "%x".format(nibble)

            if (Regex("^[a-zA-Z()]+\$").matches(c) && hashBitsValues.hasNext() && hashBitsValues.next()) {
                accum.append(c.uppercase())
            } else {
                accum.append(c.lowercase())
            }
            accum
        }.toString()
    }
}