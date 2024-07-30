package com.tangem.blockchain.common.address

interface ContractAddressValidator {

    fun validateContractAddress(address: String): Boolean
}