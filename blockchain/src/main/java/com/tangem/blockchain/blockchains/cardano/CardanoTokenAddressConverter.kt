package com.tangem.blockchain.blockchains.cardano

import com.tangem.blockchain.blockchains.binance.client.encoding.Bech32
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.blockchain.extensions.calculateBlake2b
import com.tangem.blockchain.extensions.decodeBech32
import com.tangem.common.extensions.hexToBytes

/**
 * Cardano token address converter
 *
[REDACTED_AUTHOR]
 */
class CardanoTokenAddressConverter {

    /** Convert to single format of cardano token [address] */
    @Suppress("MagicNumber")
    fun convertToFingerprint(address: String): String {
        if (address.decodeBech32() != null) return address

        val hash = address.hexToBytes().calculateBlake2b(digestByteSize = 20)

        val convertedAddressBytes = Crypto.convertBits(hash, 0, hash.size, 8, 5, true)

        return Bech32.encode("asset", convertedAddressBytes)
    }
}