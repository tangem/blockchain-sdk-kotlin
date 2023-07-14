package com.tangem.blockchain.common.address

import com.tangem.common.card.EllipticCurve

@Deprecated("Use AddressProvider.makeAddress instead of this")
interface MultipleAddressProvider : AddressService {

    fun makeAddresses(walletPublicKey: ByteArray, curve: EllipticCurve? = EllipticCurve.Secp256k1): Set<Address> {
        return setOf(PlainAddress(makeAddress(walletPublicKey, curve)))
    }
}