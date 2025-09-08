package com.tangem.blockchain.yieldlending.providers.ethereum.yield

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * protocolBalance(address yieldToken) returns (uint256)
 */
internal class EthereumYieldLendingProtocolBalanceCallData(
    private val tokenContractAddress: String,
) : SmartContractCallData {
    override val methodId: String = "0x4bd22a1b" // CHECKED
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = tokenContractAddress.hexToFixedSizeBytes()
            return prefixData + addressData
        }
}