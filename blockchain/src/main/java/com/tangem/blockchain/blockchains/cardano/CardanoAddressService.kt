package com.tangem.blockchain.blockchains.cardano

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.model.Array
import co.nstant.`in`.cbor.model.ByteString
import co.nstant.`in`.cbor.model.UnsignedInteger
import com.tangem.blockchain.blockchains.binance.client.encoding.Bech32
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.*
import com.tangem.blockchain.extensions.*
import com.tangem.common.card.EllipticCurve
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

class CardanoAddressService(private val blockchain: Blockchain) : AddressService {

    private val shelleyHeaderByte: Byte = 97

    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve,
    ): PlainAddress {
        val address = when (addressType) {
            AddressType.Default -> makeShelleyAddress(publicKey.blockchainKey)
            AddressType.Legacy -> makeByronAddress(publicKey.blockchainKey)
        }

        return PlainAddress(
            value = address,
            type = addressType,
            publicKey = publicKey
        )
    }

    override fun validate(address: String): Boolean {
        return if (isShelleyAddress(address)) {
            address.decodeBech32() != null
        } else {
            validateBase58Address(address)
        }
    }

    private fun makeByronAddress(walletPublicKey: ByteArray): String {
        val extendedPublicKey = extendPublicKey(walletPublicKey)

        val pubKeyWithAttributes = makePubKeyWithAttributes(extendedPublicKey)
        val sha3Hash = pubKeyWithAttributes.calculateSha3v256()

        val blakeHash = sha3Hash.calculateBlake2b(28)

        val hashWithAttributes = makeHashWithAttributes(blakeHash)

        return makeByronAddressWithChecksum(hashWithAttributes)
    }

    private fun makeShelleyAddress(walletPublicKey: ByteArray): String {
        val publicKeyHash = walletPublicKey.calculateBlake2b(28)

        val addressBytes = byteArrayOf(shelleyHeaderByte) + publicKeyHash
        val convertedAddressBytes =
            Crypto.convertBits(addressBytes, 0, addressBytes.size, 8, 5, true)

        return Bech32.encode(BECH32_HRP, convertedAddressBytes)
    }

    private fun validateBase58Address(address: String): Boolean {
        if (!address.startsWith("A") && !address.startsWith("D")) return false
        val decoded = address.decodeBase58() ?: return false

        return try {
            val bais = ByteArrayInputStream(decoded)
            val addressList =
                (CborDecoder(bais).decode()[0] as Array).dataItems
            val addressItemBytes = (addressList[0] as ByteString).bytes
            val checksum = (addressList[1] as UnsignedInteger).value.toLong()

            val crc32 = CRC32()
            crc32.update(addressItemBytes)
            val calculatedChecksum = crc32.value

            checksum == calculatedChecksum
        } catch (e: Exception) {
            false
        }
    }

    private fun makePubKeyWithAttributes(extendedPublicKey: ByteArray): ByteArray {
        val pubKeyWithAttributes = ByteArrayOutputStream()
        CborEncoder(pubKeyWithAttributes).encode(
            CborBuilder()
                .addArray()
                .add(0)
                .addArray()
                .add(0)
                .add(extendedPublicKey)
                .end()
                .addMap()
                .end()
                .end()
                .build()
        )
        return pubKeyWithAttributes.toByteArray()
    }

    private fun makeHashWithAttributes(blakeHash: ByteArray): ByteArray {
        val hashWithAttributes = ByteArrayOutputStream()
        CborEncoder(hashWithAttributes).encode(
            CborBuilder()
                .addArray()
                .add(blakeHash)
                .addMap() //additional attributes
                .end()
                .add(0) //address type
                .end()
                .build()
        )
        return hashWithAttributes.toByteArray()
    }

    private fun makeByronAddressWithChecksum(hashWithAttributes: ByteArray): String {
        val checksum = getCheckSum(hashWithAttributes)

        val addressItem = CborBuilder().add(hashWithAttributes).build().get(0)
        addressItem.setTag(24)

        //addr + checksum
        val address = ByteArrayOutputStream()
        CborEncoder(address).encode(
            CborBuilder()
                .addArray()
                .add(addressItem)
                .add(checksum)
                .end()
                .build()
        )

        return address.toByteArray().encodeBase58()
    }

    private fun getCheckSum(hashWithAttributes: ByteArray): Long {
        val crc32 = CRC32()
        crc32.update(hashWithAttributes)
        return crc32.value
    }

    companion object {
        const val BECH32_HRP = "addr"
        const val BECH32_SEPARATOR = "1"

        fun extendPublicKey(publicKey: ByteArray): ByteArray {
            val zeroBytes = ByteArray(32)
            return publicKey + zeroBytes
        }

        fun decode(address: String): ByteArray? {
            return if (isShelleyAddress(address)) {
                address.decodeBech32()
            } else {
                address.decodeBase58()
            }
        }

        fun isShelleyAddress(address: String) = address.startsWith(BECH32_HRP + BECH32_SEPARATOR)
    }
}
