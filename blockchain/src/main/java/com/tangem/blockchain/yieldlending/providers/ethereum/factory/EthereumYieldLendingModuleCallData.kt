package com.tangem.blockchain.yieldlending.providers.ethereum.factory

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * yieldModules(address) - get list of deployed yield modules
 */
internal class EthereumYieldLendingModuleCallData(
    private val address: String,
) : SmartContractCallData {
    override val methodId: String = "0x36571e2c" // CHECKED
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = address.hexToFixedSizeBytes()

            return prefixData + addressData
        }
}