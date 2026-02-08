package com.tangem.blockchain.common.smartcontract

internal interface Erc20CallData : SmartContractCallData {
    fun String.addressWithoutPrefix(): String = addressWithoutPrefix(this)

    companion object {
        private const val ETH_VALUABLE_ADDRESS_PART_LENGTH = 40

        internal fun addressWithoutPrefix(address: String): String {
            return address.takeLast(ETH_VALUABLE_ADDRESS_PART_LENGTH)
        }
    }
}