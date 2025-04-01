package com.tangem.blockchain.common.smartcontract

/**
 * Model for already compiled smart contract call data.
 * If some service or provider sent ready to send transaction data
 */
data class CompiledSmartContractCallData(
    override val data: ByteArray,
) : SmartContractCallData {
    override val methodId: String = ""
}