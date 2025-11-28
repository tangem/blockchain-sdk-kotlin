package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for the method that retrieves the total balance (excluding service fee) held by the protocol
 *  for a specific yield token.
 *
 *  Signature: effectiveProtocolBalance(address)
 */
internal class EthereumYieldSupplyEffectiveProtocolBalanceCallData(
    private val tokenContractAddress: String,
) : SmartContractCallData {
    override val methodId: String = "0x5002bb7e"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = tokenContractAddress.hexToFixedSizeBytes()
            return prefixData + addressData
        }
}