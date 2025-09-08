package com.tangem.blockchain.yieldlending.providers.ethereum.yield

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * yieldTokensData(address)
 */
internal class EthereumYieldLendingStatusCallData(
    private val tokenContractAddress: String,
): SmartContractCallData {
    override val methodId: String = "0xf8e8be9c" // CHECKED
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()

            return prefixData + tokenContractAddressData
        }
}