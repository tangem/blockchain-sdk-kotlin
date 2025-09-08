package com.tangem.blockchain.yieldlending.providers.ethereum.factory

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import org.kethereum.extensions.toBytesPadded

/**
 *  - **deployYieldModule(address owner, address yieldToken, uint240 maxNetworkFee)**
 *   Signature: `deployYieldModule(address,address,uint240)`
 *   Selector: `0xcbeda14c`
 */
class EthereumYieldLendingDeployCallData(
    private val address: String,
    private val tokenContractAddress: String,
    private val maxNetworkFee: Amount,
) : SmartContractCallData {
    override val methodId: String = "0xcbeda14c" // CHECKED
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = address.hexToFixedSizeBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()

            val maxFeeData = maxNetworkFee.bigIntegerValue()?.toBytesPadded(length = 32) ?: error("Invalid fee amount")

            return prefixData + addressData + tokenContractAddressData + maxFeeData
        }
}