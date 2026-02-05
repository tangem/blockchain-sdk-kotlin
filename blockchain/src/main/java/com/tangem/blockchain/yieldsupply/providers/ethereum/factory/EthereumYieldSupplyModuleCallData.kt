package com.tangem.blockchain.yieldsupply.providers.ethereum.factory

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for the method that retrieves the list of deployed Yield Module contracts
 *  associated with the specified owner address.
 *
 *  Signature: `yieldModules(address)`
 */
internal class EthereumYieldSupplyModuleCallData(
    private val address: String,
) : SmartContractCallData {
    override val methodId: String = "0x36571e2c"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = address.hexToFixedSizeBytes()

            return prefixData + addressData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(address) && address.isNotZeroAddress()
    }
}