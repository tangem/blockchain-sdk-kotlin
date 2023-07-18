package com.tangem.blockchain.common.address

import com.tangem.common.card.EllipticCurve


interface AddressService : AddressProvider, AddressValidator

interface MultisigAddressProvider {

    fun makeMultisigAddresses(
        walletPublicKey: ByteArray,
        pairPublicKey: ByteArray,
        curve: EllipticCurve? = null,
    ): Set<Address>

}