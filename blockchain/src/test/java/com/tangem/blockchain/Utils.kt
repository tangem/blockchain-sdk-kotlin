package com.tangem.blockchain

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.card.EllipticCurve

internal fun ByteArray.wrapInObject() = Wallet.PublicKey(this, null)

internal fun AddressService.makeAddressWithDefaultType(byteArray: ByteArray) : String {
    return makeAddress(Wallet.PublicKey(byteArray, null), AddressType.Default, EllipticCurve.Secp256k1).value
}

internal fun AddressService.makeAddressWithLegacyType(byteArray: ByteArray) : String {
    return makeAddress(Wallet.PublicKey(byteArray, null), AddressType.Legacy, EllipticCurve.Secp256k1).value
}