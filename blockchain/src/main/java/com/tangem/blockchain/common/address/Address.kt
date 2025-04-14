package com.tangem.blockchain.common.address

open class Address(
    val value: String,
    val type: AddressType = AddressType.Default,
)
