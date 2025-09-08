package com.tangem.blockchain.yieldlending.providers.ethereum.yield

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * - **enterProtocolByOwner(address yieldToken)**
 *   Signature: `enterProtocolByOwner(address)`
 *   Selector: `0x6b7f6d8b`
 */
class EthereumYieldLendingEnterCallData(
    private val tokenContractAddress: String,
) : SmartContractCallData {
    override val methodId: String = "0x79be55f7"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()

            return prefixData + tokenContractAddressData
        }
}