package com.tangem.blockchain.common.smartcontract

internal interface Erc20CallData : SmartContractCallData {
    fun String.addressWithoutPrefix(): String {
        return takeLast(ETH_VALUABLE_ADDRESS_PART_LENGTH)
    }

    private companion object {
        const val ETH_VALUABLE_ADDRESS_PART_LENGTH = 40
    }
}