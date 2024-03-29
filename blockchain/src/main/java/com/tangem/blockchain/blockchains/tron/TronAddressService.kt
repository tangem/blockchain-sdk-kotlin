package com.tangem.blockchain.blockchains.tron

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.blockchains.tron.libs.Base58Check
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString

class TronAddressService : AddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val decompressedPublicKey = walletPublicKey.toDecompressedPublicKey()
        val data = decompressedPublicKey.drop(1).toByteArray()
        val hash = data.toKeccak()

        val addressData = PREFIX.toByteArray(1) + hash.takeLast(ADDRESS_LENGTH - 1).toByteArray()
        return Base58Check.bytesToBase58(addressData)
    }

    override fun validate(address: String): Boolean {
        val decoded = address.decodeBase58(checked = true) ?: return false
        return decoded.count() == ADDRESS_LENGTH &&
            decoded.toHexString().startsWith(PREFIX.toByteArray(1).toHexString())
    }

    companion object {
        private const val PREFIX = 0x41
        private const val ADDRESS_LENGTH = 21

        fun toHexForm(base58String: String, length: Int? = null): String? {
            val hex = base58String.decodeBase58(checked = true)?.toHexString() ?: return null
            return if (length != null) {
                hex.padStart(length, '0')
            } else {
                hex
            }
        }
    }
}
