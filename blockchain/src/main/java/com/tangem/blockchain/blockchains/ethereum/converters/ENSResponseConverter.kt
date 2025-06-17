package com.tangem.blockchain.blockchains.ethereum.converters

import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.extensions.hexToInt

@Suppress("MagicNumber")
internal object ENSResponseConverter {

    /**
     * Converts the ENS response string to an Ethereum address.
     *
     * @param result The ENS response string.
     * @return The Ethereum address as a hex string.
     *
     * input example:
     * "0x" +
     * "0000000000000000000000000000000000000000000000000000000000000040" +
     * "000000000000000000000000231b0ee14048e9dccd1d247744d114a4eb5e8e63" +
     * "0000000000000000000000000000000000000000000000000000000000000020" +
     * "000000000000000000000000d8da6bf26964af9d7eed9e03e53415d37aa96045"
     *
     * output example:
     * "0xd8da6bf26964af9d7eed9e03e53415d37aa96045"
     */
    fun convert(result: String): String {
        val clean = result.removePrefix(HEX_PREFIX)

        val offsetHex = clean.substring(0, 64)
        val offset = offsetHex.hexToInt() * 2

        val lengthHex = clean.substring(offset, offset + 64)
        val length = lengthHex.hexToInt()

        val dataStart = offset + 64
        val dataEnd = dataStart + length * 2
        val dataHex = clean.substring(dataStart, dataEnd)

        val addressHex = dataHex.takeLast(40)
        return "0x$addressHex"
    }
}