package com.tangem.blockchain.yieldlending.providers.ethereum.yield

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * effectiveBalance(address yieldToken) returns (uint256)
 */
internal class EthereumYieldLendingBalanceCallData(
    private val tokenContractAddress: String,
) : SmartContractCallData {
    override val methodId: String = "0x16a398f7" // CHECKED
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = tokenContractAddress.hexToFixedSizeBytes()
            return prefixData + addressData
        }
}