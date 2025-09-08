package com.tangem.blockchain.yieldlending.providers.ethereum.yield

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  - **reactivateToken(address yieldToken)**
 *   Signature: `reactivateToken(address)`
 *   Selector: `0x4b9e0d2f`
 */
class EthereumYieldLendingReactivateTokenCallData(
    private val tokenContractAddress: String,
) : SmartContractCallData {
    override val methodId: String = "0x0d31916f"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()

            return prefixData + tokenContractAddressData
        }
}