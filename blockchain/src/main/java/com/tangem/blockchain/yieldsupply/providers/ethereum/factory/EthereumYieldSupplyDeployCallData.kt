package com.tangem.blockchain.yieldsupply.providers.ethereum.factory

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import org.kethereum.extensions.toBytesPadded

/**
 *  Call data for the method that deploys a new Yield Module contract
 *  for the specified owner address and token contract address.
 *
 *  Signature: `deployYieldModule(address,address,uint240)`
 */
internal class EthereumYieldSupplyDeployCallData(
    private val address: String,
    private val tokenContractAddress: String,
    private val maxNetworkFee: Amount,
) : SmartContractCallData {
    override val methodId: String = "0xcbeda14c"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = address.hexToFixedSizeBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()

            val maxFeeData = maxNetworkFee.bigIntegerValue()?.toBytesPadded(length = 32) ?: error("Invalid fee amount")

            return prefixData + addressData + tokenContractAddressData + maxFeeData
        }
}