package com.tangem.blockchain.common.address

abstract class AddressService {
    abstract fun makeAddress(walletPublicKey: ByteArray): String
    abstract fun validate(address: String): Boolean
    open fun makeAddresses(walletPublicKey: ByteArray): Set<Address> =
            setOf(Address(makeAddress(walletPublicKey)))
}

interface MultisigAddressProvider {
    fun makeMultisigAddress(walletPublicKey: ByteArray, pairPublicKey: ByteArray): Set<Address>
}