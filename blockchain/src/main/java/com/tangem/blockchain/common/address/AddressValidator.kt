package com.tangem.blockchain.common.address

interface AddressValidator {

    fun validate(address: String): Boolean

}