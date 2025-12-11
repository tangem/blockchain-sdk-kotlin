package com.tangem.blockchain.blockchains.bitcoin.walletconnect

/**
 * Encoder for converting Bitcoin signatures to DER format.
 *
 * DER (Distinguished Encoding Rules) is a binary format used for ECDSA signatures in Bitcoin.
 * This encoder handles the conversion of raw 64-byte signatures (32-byte R + 32-byte S)
 * into properly formatted DER-encoded signatures with sighash type appended.
 *
 * DER encoding format:
 * ```
 * 0x30 [total-length] 0x02 [r-length] [r] 0x02 [s-length] [s] [sighash-type]
 * ```
 *
 * @see <a href="https://en.wikipedia.org/wiki/X.690#DER_encoding">DER Encoding</a>
 */
internal object DerSignatureEncoder {

    private const val SIGNATURE_SIZE = 64
    private const val R_SIZE = 32
    private const val S_SIZE = 32
    private const val DER_SEQUENCE_TAG: Byte = 0x30
    private const val DER_INTEGER_TAG: Byte = 0x02
    private const val HIGH_BIT_MASK = 0x80

    /**
     * Encodes a signature in DER format with sighash type appended.
     *
     * @param signature Raw 64-byte signature (32-byte R + 32-byte S)
     * @param sighashType Sighash type to append (e.g., SIGHASH_ALL = 0x01)
     * @return DER-encoded signature with sighash type appended
     * @throws IllegalArgumentException if signature is not 64 bytes
     */
    fun encodeDerSignature(signature: ByteArray, sighashType: Byte): ByteArray {
        require(signature.size == SIGNATURE_SIZE) {
            "Signature must be $SIGNATURE_SIZE bytes ($R_SIZE r + $S_SIZE s)"
        }

        val (r, s) = extractRandS(signature)
        val derSignature = encodeDer(r, s)

        return derSignature + sighashType
    }

    /**
     * Extracts R and S components from signature.
     */
    private fun extractRandS(signature: ByteArray): Pair<ByteArray, ByteArray> {
        val r = signature.copyOfRange(0, R_SIZE)
        val s = signature.copyOfRange(R_SIZE, SIGNATURE_SIZE)
        return r to s
    }

    /**
     * Encodes r and s values in DER format.
     *
     * DER encoding for ECDSA signatures:
     * - 0x30 [total-length] 0x02 [r-length] [r] 0x02 [s-length] [s]
     *
     * @param r R component of signature
     * @param s S component of signature
     * @return DER-encoded signature
     */
    private fun encodeDer(r: ByteArray, s: ByteArray): ByteArray {
        val rEncoded = encodeIntegerDer(r)
        val sEncoded = encodeIntegerDer(s)
        val content = rEncoded + sEncoded

        return byteArrayOf(DER_SEQUENCE_TAG, content.size.toByte()) + content
    }

    /**
     * Encodes a single integer in DER format.
     */
    private fun encodeIntegerDer(value: ByteArray): ByteArray {
        val trimmed = trimLeadingZeros(value)
        val encoded = addPaddingIfNeeded(trimmed)

        return byteArrayOf(DER_INTEGER_TAG, encoded.size.toByte()) + encoded
    }

    /**
     * Trims leading zeros from byte array.
     */
    private fun trimLeadingZeros(value: ByteArray): ByteArray {
        val trimmed = value.dropWhile { it == 0.toByte() }.toByteArray()
        return if (trimmed.isEmpty()) byteArrayOf(0) else trimmed
    }

    /**
     * Adds padding if high bit is set.
     */
    private fun addPaddingIfNeeded(value: ByteArray): ByteArray {
        val needsPadding = value[0].toInt() and HIGH_BIT_MASK != 0
        return if (needsPadding) byteArrayOf(0) + value else value
    }
}