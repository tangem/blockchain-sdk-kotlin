package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for the method that allows the owner of a specific yield token
 *  to withdraw their funds and deactivate their participation in the protocol associated with that token.
 *
 *  Signature: `withdrawAndDeactivate(address)`
 */
class EthereumYieldSupplyExitCallData(
    private val tokenContractAddress: String,
) : SmartContractCallData {
    override val methodId: String = "0xc65e6dcf"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()

            return prefixData + tokenContractAddressData
        }
}