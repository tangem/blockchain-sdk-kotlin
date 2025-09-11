package com.tangem.blockchain.blockchains.ethereum.converters

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.ADDRESS_HEX_LENGTH
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.HEX_CHARS_PER_BYTE
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.WORD_HEX_LENGTH
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.extensions.hexToInt

@Suppress("MagicNumber")
internal object ENSResponseConverter {

    /**
     * Converts the ENS response string to an Ethereum address.
     *
     * @param result The ENS response string in hex.
     * @return The Ethereum address as a hex string, e.g. "0xd8da6bf26964af9d7eed9e03e53415d37aa96045"
     *
     * Example input:
     * "0x" +
     * "0000000000000000000000000000000000000000000000000000000000000040" +
     * "000000000000000000000000231b0ee14048e9dccd1d247744d114a4eb5e8e63" +
     * "0000000000000000000000000000000000000000000000000000000000000020" +
     * "000000000000000000000000d8da6bf26964af9d7eed9e03e53415d37aa96045"
     */
    fun convert(result: String): String {
        val clean = result.removePrefix(HEX_PREFIX)

        val offsetHex = clean.substring(0, WORD_HEX_LENGTH)
        val offsetBytes = offsetHex.hexToInt()
        val offset = offsetBytes * HEX_CHARS_PER_BYTE

        val lengthHex = clean.substring(offset, offset + WORD_HEX_LENGTH)
        val lengthBytes = lengthHex.hexToInt()
        val dataStart = offset + WORD_HEX_LENGTH
        val dataEnd = dataStart + lengthBytes * HEX_CHARS_PER_BYTE

        val dataHex = clean.substring(dataStart, dataEnd)
        val addressHex = dataHex.takeLast(ADDRESS_HEX_LENGTH)

        return "$HEX_PREFIX$addressHex"
    }
}