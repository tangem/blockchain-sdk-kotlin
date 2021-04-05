package com.tangem.blockchain.common.address

import com.tangem.commands.common.card.EllipticCurve

abstract class AddressService {
    abstract fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve? = null): String
    abstract fun validate(address: String): Boolean
    open fun makeAddresses(walletPublicKey: ByteArray, curve: EllipticCurve? = null): Set<Address> =
            setOf(Address(makeAddress(walletPublicKey, curve)))
}

interface MultisigAddressProvider {
    fun makeMultisigAddresses(
            walletPublicKey: ByteArray,
            pairPublicKey: ByteArray,
            curve: EllipticCurve? = null
    ): Set<Address>
}