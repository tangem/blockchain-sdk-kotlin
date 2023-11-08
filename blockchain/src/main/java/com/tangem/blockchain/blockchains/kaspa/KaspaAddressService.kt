package com.tangem.blockchain.blockchains.kaspa

import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaAddressType
import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaCashAddr
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toCompressedPublicKey

class KaspaAddressService : AddressService {

    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve,
    ): PlainAddress {
        val pk = publicKey.blockchainKey.toCompressedPublicKey()
        return PlainAddress(
            value = KaspaCashAddr.toCashAddress(KaspaAddressType.P2PK_ECDSA, pk),
            type = addressType,
            publicKey = publicKey
        )
    }

    override fun validate(address: String): Boolean {
        return KaspaCashAddr.isValidCashAddress(address)
    }

    fun getPublicKey(address: String): ByteArray {
        return KaspaCashAddr.decodeCashAddress(address).hash
    }
}