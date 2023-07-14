package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Wallet
import com.tangem.common.card.EllipticCurve
// [REDACTED_TODO_COMMENT]
interface AddressService : AddressProvider {

    // address making, old style
    fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve? = EllipticCurve.Secp256k1): String

    fun validate(address: String): Boolean

    override fun makeAddress(publicKey: Wallet.PublicKey, addressType: AddressType): AddressPublicKeyPair {
// [REDACTED_TODO_COMMENT]
        val address = makeAddress(publicKey.blockchainKey)

        return AddressPublicKeyPair(address, publicKey, addressType)
    }

    // we need start use this method and smoothly move to this
    // remove "newStyle" later
    // made this way because return type is not signature part
    fun makeAddressNewStyle(walletPublicKey: ByteArray, curve: EllipticCurve? = EllipticCurve.Secp256k1): Address {
        return PlainAddress(makeAddress(walletPublicKey, curve))
    }

}


interface MultisigAddressProvider {

    fun makeMultisigAddresses(
        walletPublicKey: ByteArray,
        pairPublicKey: ByteArray,
        curve: EllipticCurve? = null,
    ): Set<Address>

}