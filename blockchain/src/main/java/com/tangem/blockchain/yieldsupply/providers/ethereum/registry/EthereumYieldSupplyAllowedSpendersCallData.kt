package com.tangem.blockchain.yieldsupply.providers.ethereum.registry

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for checking if a spender address is allowed in SwapExecutionRegistry.
 *
 *  Signature: `allowedSpenders(address spender)`
 */
class EthereumYieldSupplyAllowedSpendersCallData(
    private val spenderAddress: String,
) : SmartContractCallData {
    override val methodId: String = METHOD_ID
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val spenderData = spenderAddress.hexToFixedSizeBytes()
            return prefixData + spenderData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(spenderAddress) && spenderAddress.isNotZeroAddress()
    }

    companion object {
        const val METHOD_ID = "0xd8528af0"
    }
}