package com.tangem.blockchain.common.address

import com.tangem.blockchain.blockchains.ethereum.EthereumDerivationData
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
        derivationPath: String?,
        cachedIndex: Int?,
    ): EthereumDerivationData {
        return EthereumDerivationData(
            address = makeAddress(extendedPublicKey.publicKey, curve),
            path = null,
            publicKey = extendedPublicKey,
            index = null,
        )
    }
}

interface MultisigAddressProvider {
    fun makeMultisigAddresses(
        walletPublicKey: ByteArray,
        pairPublicKey: ByteArray,
        curve: EllipticCurve? = null,
    ): Set<Address>
}