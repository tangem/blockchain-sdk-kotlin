package com.tangem.blockchain.yieldlending.providers.ethereum.processor

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.common.extensions.hexToBytes

/**
 * serviceFeeRate() - tangem fee rate
 */
internal object EthereumYieldLendingServiceFeeCallData : SmartContractCallData {
    override val methodId: String = "0x61d1bc94" // CHECKED

    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            return prefixData
        }
}