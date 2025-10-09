package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import org.kethereum.extensions.toBytesPadded

/**
 *  Call data for the method that initializes a yield token within the yield module protocol
 *
 *  Signature: `initYieldToken(address,uint240)`
 */
class EthereumYieldSupplyInitTokenCallData(
    private val tokenContractAddress: String,
    private val maxNetworkFee: Amount,
) : SmartContractCallData {
    override val methodId: String = "0xebd4b81c"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()
            val maxFeeData = maxNetworkFee.bigIntegerValue()?.toBytesPadded(length = 32) ?: error("Invalid fee amount")

            return prefixData + tokenContractAddressData + maxFeeData
        }
}