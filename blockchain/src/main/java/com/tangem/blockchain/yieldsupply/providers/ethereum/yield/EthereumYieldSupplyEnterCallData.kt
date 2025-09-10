package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for the method that allows the owner of a specific yield token
 *  to enter the protocol associated with that token.
 *
 *  Signature: `enterProtocolByOwner(address)`
 */
internal class EthereumYieldSupplyEnterCallData(
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