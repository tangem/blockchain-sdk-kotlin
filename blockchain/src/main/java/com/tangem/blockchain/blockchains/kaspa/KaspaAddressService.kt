package com.tangem.blockchain.blockchains.kaspa

import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaAddressType
import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaCashAddr
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toCompressedPublicKey

class KaspaAddressService : AddressService() {
    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val publicKey = walletPublicKey.toCompressedPublicKey()
        return KaspaCashAddr.toCashAddress(KaspaAddressType.P2PK_ECDSA, publicKey)
    }

    override fun validate(address: String): Boolean {
        return KaspaCashAddr.isValidCashAddress(address)
    }

    fun getPublicKey(address: String): ByteArray {
        return KaspaCashAddr.decodeCashAddress(address).hash
    }
}
