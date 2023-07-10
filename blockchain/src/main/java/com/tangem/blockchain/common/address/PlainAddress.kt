package com.tangem.blockchain.common.address

data class PlainAddress(
    override val value: String,
    override val type: AddressType = AddressType.Default
) : Address(value, type)