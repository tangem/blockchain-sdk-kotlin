package com.tangem.blockchain.blockchains.decimal

import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toDecompressedPublicKey
import org.bitcoinj.core.Bech32
import org.kethereum.crypto.toAddress
import org.kethereum.erc55.hasValidERC55ChecksumOrNoChecksum
import org.kethereum.erc55.withERC55Checksum
import org.kethereum.model.Address
import org.kethereum.model.PublicKey
import org.komputing.khex.extensions.toHexString

internal class DecimalAddressService : AddressService {

    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve,
    ): PlainAddress {
        return if (addressType == AddressType.Default) {
            PlainAddress(
                value = makeErcAddress(publicKey.blockchainKey),
                type = AddressType.Default,
                publicKey = publicKey
            )
        } else {
            PlainAddress(
                value =  convertErcAddressToDscAddress(makeErcAddress(publicKey.blockchainKey)),
                type = AddressType.Legacy,
                publicKey = publicKey
            )
        }
    }

    private fun makeErcAddress(walletPublicKey: ByteArray): String {
        val decompressedPublicKey = walletPublicKey
            .toDecompressedPublicKey()
            .sliceArray(1..64)

        return PublicKey(decompressedPublicKey)
            .toAddress()
            .withERC55Checksum()
            .hex
    }

    override fun validate(address: String): Boolean {
        val addressToValidate = when {
            address.startsWith(ADDRESS_PREFIX) || address.startsWith(LEGACY_ADDRESS_PREFIX) -> {
                convertDscAddressToErcAddress(address) ?: return false
            }

            else -> address
        }

        return Address(addressToValidate).hasValidERC55ChecksumOrNoChecksum()
    }

    companion object {
        private const val ADDRESS_PREFIX = "d0"
        private const val LEGACY_ADDRESS_PREFIX = "dx"
        private const val ERC55_ADDRESS_PREFIX = "0x"

        fun convertDscAddressToErcAddress(addressHex: String): String? {
            if (addressHex.startsWith(ERC55_ADDRESS_PREFIX)) {
                return addressHex
            }

            val (prefix, addressBytes) = Bech32.decode(addressHex).let { it.hrp to it.data }
            if (prefix == null || addressBytes == null) return null

            val convertedAddressBytes = Crypto.convertBits(addressBytes, 0, addressBytes.size, 5, 8, false)

            return convertedAddressBytes.toHexString()
        }

        fun convertErcAddressToDscAddress(addressHex: String): String {
            if (addressHex.startsWith(ADDRESS_PREFIX) || addressHex.startsWith(LEGACY_ADDRESS_PREFIX)) {
                return addressHex
            }

            val addressBytes = addressHex.hexToBytes()
            val converted = Crypto.convertBits(addressBytes, 0, addressBytes.size, 8, 5, false)

            return Bech32.encode(ADDRESS_PREFIX, converted)
        }
    }
}
