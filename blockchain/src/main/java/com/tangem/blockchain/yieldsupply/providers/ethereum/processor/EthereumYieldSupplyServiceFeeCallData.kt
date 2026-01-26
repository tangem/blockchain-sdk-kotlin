package com.tangem.blockchain.yieldsupply.providers.ethereum.processor

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for the method that retrieves the service fee rate from the Tangem's Yield Module contract.
 *
 *  Signature: serviceFeeRate()
 */
internal object EthereumYieldSupplyServiceFeeCallData : SmartContractCallData {
    override val methodId: String = "0x61d1bc94"

    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            return prefixData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return true
    }
}