package com.tangem.blockchain.blockchains.cardano

import com.tangem.blockchain.blockchains.binance.client.encoding.Bech32
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.blockchain.blockchains.cardano.utils.CardanoContractAddressRecognizer
import com.tangem.blockchain.extensions.calculateBlake2b
import com.tangem.blockchain.extensions.hexToBytesOrNull
import com.tangem.common.extensions.toHexString
import com.tangem.blockchain.blockchains.cardano.utils.CardanoContractAddressRecognizer.Address as CardanoContractAddress

/**
 * Cardano token address converter
 *
 * @author Andrew Khokhlov on 26/06/2024
 */
class CardanoTokenAddressConverter {

    /**
     * Convert to single format of cardano token address or null if address is not valid
     *
     * @param address token contract address (PolicyID or Fingerprint or AssetID)
     * @param symbol  token symbol, aka asset name
     */
    @Suppress("MagicNumber")
    fun convertToFingerprint(address: String, symbol: String? = null): String? {
        val recognizedAddress = CardanoContractAddressRecognizer.recognize(address) ?: return null

        if (recognizedAddress is CardanoContractAddress.Fingerprint) return address

        val preparedAddress = if (recognizedAddress is CardanoContractAddress.PolicyID) {
            address + symbol?.toByteArray()?.toHexString().orEmpty()
        } else {
            address
        }

        val hash = preparedAddress.hexToBytesOrNull()
            ?.calculateBlake2b(digestByteSize = 20)
            ?: return null

        val convertedAddressBytes = Crypto.convertBits(hash, 0, hash.size, 8, 5, true)

        return Bech32.encode(CardanoUtils.CARDANO_FINGERPRINT_ADDRESS_PREFIX, convertedAddressBytes)
    }
}
