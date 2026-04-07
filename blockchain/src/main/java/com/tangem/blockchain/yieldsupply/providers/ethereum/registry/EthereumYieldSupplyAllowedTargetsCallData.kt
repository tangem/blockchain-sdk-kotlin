package com.tangem.blockchain.yieldsupply.providers.ethereum.registry

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for checking if a target address is allowed in SwapExecutionRegistry.
 *
 *  Signature: `allowedTargets(address target)`
 */
class EthereumYieldSupplyAllowedTargetsCallData(
    private val targetAddress: String,
) : SmartContractCallData {
    override val methodId: String = METHOD_ID
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val targetData = targetAddress.hexToFixedSizeBytes()
            return prefixData + targetData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(targetAddress) && targetAddress.isNotZeroAddress()
    }

    companion object {
        const val METHOD_ID = "0xb8fe8d5f"
    }
}