package com.tangem.blockchain.common.address

import android.content.Context
import com.tangem.common.card.EllipticCurve

abstract class AddressService {
    abstract fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve? = EllipticCurve.Secp256k1): String
    abstract fun validate(address: String): Boolean
    open fun makeAddresses(walletPublicKey: ByteArray, curve: EllipticCurve? = EllipticCurve.Secp256k1): Set<Address> =
        setOf(Address(makeAddress(walletPublicKey, curve)))
}

interface ContextAddressProvider {
    fun makeContextAddresses(
        walletPublicKey: ByteArray,
        curve: EllipticCurve? = EllipticCurve.Secp256k1,
        context: Context
    ): Set<Address>
}

interface MultisigAddressProvider {
    fun makeMultisigAddresses(
        walletPublicKey: ByteArray,
        pairPublicKey: ByteArray,
        curve: EllipticCurve? = null,
    ): Set<Address>
}