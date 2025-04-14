package com.tangem.blockchain.blockchains.decimal

import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
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
import com.tangem.blockchain.common.address.Address as SdkAddress

internal class DecimalAddressService : AddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return makeDscAddress(walletPublicKey)
    }

    override fun makeAddresses(walletPublicKey: ByteArray, curve: EllipticCurve?): Set<SdkAddress> {
        val dscAddress = makeDscAddress(walletPublicKey)

        return setOf(
            SdkAddress(dscAddress, AddressType.Legacy),
            SdkAddress(convertDscAddressToDelAddress(dscAddress), AddressType.Default),
        )
    }

    /** Same as ERC55 address */
    @Suppress("MagicNumber")
    private fun makeDscAddress(walletPublicKey: ByteArray): String {
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
                convertDelAddressToDscAddress(address)
            }

            else -> address
        }

        return Address(addressToValidate).hasValidERC55ChecksumOrNoChecksum()
    }

    companion object {
        private const val ADDRESS_PREFIX = "d0"
        private const val LEGACY_ADDRESS_PREFIX = "dx"
        private const val ERC55_ADDRESS_PREFIX = "0x"

        @Suppress("MagicNumber")
        fun convertDelAddressToDscAddress(addressHex: String): String {
            if (addressHex.startsWith(ERC55_ADDRESS_PREFIX)) {
                return addressHex
            }

            val (prefix, addressBytes) = Bech32.decode(addressHex).let { it.hrp to it.data }
            require(value = prefix != null && addressBytes != null) {
                "Unable to convert DEL address to DSC address: $addressHex"
            }

            val convertedAddressBytes = Crypto.convertBits(addressBytes, 0, addressBytes.size, 5, 8, false)

            return convertedAddressBytes.toHexString()
        }

        @Suppress("MagicNumber")
        fun convertDscAddressToDelAddress(addressHex: String): String {
            if (addressHex.startsWith(ADDRESS_PREFIX) || addressHex.startsWith(LEGACY_ADDRESS_PREFIX)) {
                return addressHex
            }

            val addressBytes = addressHex.hexToBytes()
            val converted = Crypto.convertBits(addressBytes, 0, addressBytes.size, 8, 5, false)

            return Bech32.encode(ADDRESS_PREFIX, converted)
        }
    }
}
