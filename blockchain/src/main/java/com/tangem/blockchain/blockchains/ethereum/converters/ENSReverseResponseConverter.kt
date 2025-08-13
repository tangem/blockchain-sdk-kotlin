package com.tangem.blockchain.blockchains.ethereum.converters

import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.extensions.hexToInt

@Suppress("MagicNumber")
internal object ENSReverseResponseConverter {

    private const val WORD_BYTES = 32
    private const val HEX_CHARS_PER_BYTE = 2
    private const val WORD_HEX_LENGTH = WORD_BYTES * HEX_CHARS_PER_BYTE // 64

    /**
     * Converts the ENS reverse response string to an ENS name.
     *
     * @param result The ENS reverse response string in hex.
     * @return The ENS name as a string, e.g. "vitalik.eth"
     *
     * Example input:
     * "0x" +
     * "0000000000000000000000000000000000000000000000000000000000000060" + // offset to bytes
     * "000000000000000000000000231b0ee14048e9dccd1d247744d114a4eb5e8e63" + // forward resolver address
     * "0000000000000000000000005fbb459c49bb06083c33109fa4f14810ec2cf358" + // reverse resolver address
     * "000000000000000000000000000000000000000000000000000000000000000b" + // length of ens name bytes
     * "766974616c696b2e657468000000000000000000000000000000000000000000" // ens name bytes
     */
    fun convert(result: String): String {
        val clean = result.removePrefix(HEX_PREFIX)

        val offsetHex = clean.substring(0, WORD_HEX_LENGTH)
        val offsetBytes = offsetHex.hexToInt()
        val offsetHexChars = offsetBytes * HEX_CHARS_PER_BYTE

        val lengthEnd = offsetHexChars + WORD_HEX_LENGTH
        if (lengthEnd > clean.length) {
            throw IllegalArgumentException(
                "Response too short to contain length field at position $lengthEnd, " +
                    "but response length is ${clean.length}",
            )
        }
        val lengthHex = clean.substring(offsetHexChars, lengthEnd)
        val nameLengthBytes = lengthHex.hexToInt()
        val nameLengthHexChars = nameLengthBytes * HEX_CHARS_PER_BYTE

        val nameEnd = lengthEnd + nameLengthHexChars
        if (nameEnd > clean.length) {
            throw IllegalArgumentException(
                "Response too short to contain name bytes. Need $nameEnd " +
                    "chars but only have ${clean.length}",
            )
        }

        val nameBytesHex = clean.substring(lengthEnd, nameEnd)

        val nameBytes = ByteArray(nameLengthBytes)
        for (i in 0 until nameLengthBytes) {
            val hexStart = i * 2
            val hexPair = nameBytesHex.substring(hexStart, hexStart + 2)
            nameBytes[i] = hexPair.toInt(16).toByte()
        }
        return String(nameBytes, Charsets.UTF_8)
    }
}