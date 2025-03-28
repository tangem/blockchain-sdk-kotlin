package com.tangem.blockchain.common.smartcontract

/**
 * Model for already compiled smart contract data.
 * If some service or provider sent ready to send transaction data
 */
data class CompiledSmartContractMethod(
    override val data: ByteArray,
) : SmartContractMethod {
    override val prefix: String = ""
}