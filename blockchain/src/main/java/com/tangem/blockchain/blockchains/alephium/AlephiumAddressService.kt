package com.tangem.blockchain.blockchains.alephium

import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toCompressedPublicKey
import org.spongycastle.jcajce.provider.digest.Blake2b

/**
 * https://github.com/alephium/alephium-web3/blob/master/packages/web3/src/address/address.ts
 */
internal class AlephiumAddressService : AddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val publicKey = walletPublicKey.toCompressedPublicKey()
        val blake256WithPrefix = byteArrayOf(P2PKH_ADDRESS_PREFIX) + Blake2b.Blake2b256().digest(publicKey)
        val address = blake256WithPrefix.encodeBase58(checked = false)
        return address
    }

    override fun validate(address: String): Boolean {
        val decoded = address.decodeBase58(false) ?: return false
        val addressType = decoded.getOrNull(0) ?: return false
        return addressType == P2PKH_ADDRESS_PREFIX && decoded.size == DECODED_ADDRESS_LENGTH
    }

    companion object {
        private const val P2PKH_ADDRESS_PREFIX: Byte = 0x00
        private const val DECODED_ADDRESS_LENGTH: Int = 33
    }
}