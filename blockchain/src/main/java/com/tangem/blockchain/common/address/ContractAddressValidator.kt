package com.tangem.blockchain.common.address

/**
 * Contract address validation abstraction
 *
 * Some blockchains have different contract address format than wallet address
 */
interface ContractAddressValidator {

    /**
     * Solution to fix contract address if it is trimmed
     */
    fun tryFixContractAddress(address: String?): String? = address

    fun validateContractAddress(address: String): Boolean
}