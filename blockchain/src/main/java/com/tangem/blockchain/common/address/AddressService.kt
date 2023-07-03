package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet
import com.tangem.common.card.EllipticCurve

interface AddressService : AddressProvider {

    // address making, old style
    fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve? = EllipticCurve.Secp256k1): String

    fun validate(address: String): Boolean

    override fun makeAddress(publicKey: Wallet.PublicKey, addressType: AddressType): AddressPublicKeyPair {
        //TODO() check publicKey.blockchainKey
        val address = makeAddress(publicKey.blockchainKey)

        return AddressPublicKeyPair(address, publicKey, addressType)
    }

    // we need start use this method and smoothly move to this
    // remove "newStyle" later
    // made this way because return type is not signature part
    fun makeAddressNewStyle(walletPublicKey: ByteArray, curve: EllipticCurve? = EllipticCurve.Secp256k1): Address {
        return Address(makeAddress(walletPublicKey, curve))
    }

}

interface AddressProvider {
    fun makeAddress(publicKey: Wallet.PublicKey, addressType: AddressType): AddressPublicKeyPair
}


@Deprecated("Use AddressProvider.makeAddress instead of this")
interface MultipleAddressProvider : AddressService {

    fun makeAddresses(walletPublicKey: ByteArray, curve: EllipticCurve? = EllipticCurve.Secp256k1): Set<Address> {
        return setOf(Address(makeAddress(walletPublicKey, curve)))
    }
}

interface MultisigAddressProvider {

    fun makeMultisigAddresses(
        walletPublicKey: ByteArray,
        pairPublicKey: ByteArray,
        curve: EllipticCurve? = null,
    ): Set<Address>
}