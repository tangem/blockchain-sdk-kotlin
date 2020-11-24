package com.tangem.blockchain.common.address

open class Address(
        val value: String,
        val type: AddressType = DefaultAddressType
)

interface AddressType {
    val displayNameRes: Int
}

object DefaultAddressType : AddressType {
    override val displayNameRes = 0 //TODO: change to string resource
}