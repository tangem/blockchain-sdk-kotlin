package com.tangem.blockchain.yieldlending.providers.ethereum.yield

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  - **withdrawAndDeactivate(address yieldToken)**
 *   Signature: `withdrawAndDeactivate(address)`
 *   Selector: `0x2e1a7d4d`
 */
class EthereumYieldLendingExitCallData(
    private val tokenContractAddress: String,
) : SmartContractCallData {
    override val methodId: String = "0xc65e6dcf"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()

            return prefixData + tokenContractAddressData
        }
}