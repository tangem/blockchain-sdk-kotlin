package com.tangem.blockchain.blockchains.xrp

import com.ripple.encodings.addresses.Addresses
import com.ripple.encodings.base58.B58
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey
import org.kethereum.extensions.toBigInteger

class XrpAddressService : AddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val canonicalPublicKey = canonizePublicKey(walletPublicKey)
        val publicKeyHash = canonicalPublicKey.calculateSha256().calculateRipemd160()
        return Addresses.encodeAccountID(publicKeyHash)
    }

    override fun validate(address: String): Boolean {
        return when {
            address.startsWith("r") -> try {
                Addresses.decodeAccountID(address)
                true
            } catch (excpetion: Exception) {
                false
            }
            address.startsWith("X") -> decodeXAddress(address) != null
            else -> false
        }
    }

    @Suppress("MagicNumber")
    companion object {
        private val xrpBase58 = B58("rpshnaf39wBUDNEGHJKLM4PQRST7VWXYZ2bcdeCg65jkm8oFqi1tuvAxyz")
        private val xAddressMainnetPrefix = byteArrayOf(0x05, 0x44)
        private val zeroTagBytes = ByteArray(4) { 0 }

        fun canonizePublicKey(publicKey: ByteArray): ByteArray {
            val compressedPublicKey = publicKey.toCompressedPublicKey()
            return if (compressedPublicKey.size == 32) {
                byteArrayOf(0xED.toByte()) + compressedPublicKey
            } else {
                compressedPublicKey
            }
        }

        @Suppress("MagicNumber")
        fun decodeXAddress(address: String): XrpTaggedAddress? {
            try {
                val addressBytes = xrpBase58.decodeChecked(address)
                if (addressBytes.size != 31) return null

                val prefix = addressBytes.slice(0..1).toByteArray()
                if (!prefix.contentEquals(xAddressMainnetPrefix)) return null

                val accountBytes = addressBytes.slice(2..21).toByteArray()
                val classicAddress = Addresses.encodeAccountID(accountBytes)

                val flag = addressBytes[22]

                val tagBytes = addressBytes.slice(23..26).toByteArray()
                val reservedTagBytes = addressBytes.slice(27..30).toByteArray()
                if (!reservedTagBytes.contentEquals(zeroTagBytes)) return null

                var tag: Long? = null
                when (flag) {
                    0.toByte() -> if (!tagBytes.contentEquals(zeroTagBytes)) return null
                    1.toByte() -> {
                        tag = tagBytes.reversedArray().toBigInteger().toLong()
                    }
                    else -> return null
                }
                return XrpTaggedAddress(classicAddress, tag)
            } catch (e: Exception) {
                return null
            }
        }
    }
}

data class XrpTaggedAddress(
    val address: String,
    val destinationTag: Long?,
)
