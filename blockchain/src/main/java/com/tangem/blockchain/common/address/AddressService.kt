package com.tangem.blockchain.common.address

import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey

abstract class AddressService {
    abstract fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve? = EllipticCurve.Secp256k1): String
    abstract fun validate(address: String): Boolean
    open fun makeAddresses(walletPublicKey: ByteArray, curve: EllipticCurve? = EllipticCurve.Secp256k1): Set<Address> =
        setOf(Address(makeAddress(walletPublicKey, curve)))

    open fun makeAddressFromExtendedPublicKey(
        extendedPublicKey: ExtendedPublicKey,
        curve: EllipticCurve? = EllipticCurve.Secp256k1,
    ): String {
        return makeAddress(extendedPublicKey.publicKey, curve)
    }
}

interface MultisigAddressProvider {
    fun makeMultisigAddresses(
        walletPublicKey: ByteArray,
        pairPublicKey: ByteArray,
        curve: EllipticCurve? = null,
    ): Set<Address>
}