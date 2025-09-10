package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for the method that retrieves yield token status information
 *
 *  Signature: yieldTokensData(address)
 */
internal class EthereumYieldSupplyStatusCallData(
    private val tokenContractAddress: String,
) : SmartContractCallData {
    override val methodId: String = "0xf8e8be9c"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()

            return prefixData + tokenContractAddressData
        }
}