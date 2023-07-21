package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet
import com.tangem.common.card.EllipticCurve

interface AddressProvider {

    fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve
    ): PlainAddress

}

// address making, old style
fun AddressProvider.makeAddressOldStyle(
    walletPublicKey: ByteArray,
    addressType: AddressType = AddressType.Default,
    curve: EllipticCurve = EllipticCurve.Secp256k1
): PlainAddress {
    return makeAddress(
        publicKey = Wallet.PublicKey(walletPublicKey, null),
        addressType = addressType,
        curve = curve
    )
}

