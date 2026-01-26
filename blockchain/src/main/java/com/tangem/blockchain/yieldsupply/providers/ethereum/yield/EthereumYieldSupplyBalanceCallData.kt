package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for the method that retrieves the effective balance of a specific yield token
 *
 *  Signature: effectiveBalanceOf(address)
 */
internal class EthereumYieldSupplyBalanceCallData(
    private val tokenContractAddress: String,
) : SmartContractCallData {
    override val methodId: String = "0x16a398f7"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = tokenContractAddress.hexToFixedSizeBytes()
            return prefixData + addressData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(tokenContractAddress) && tokenContractAddress.isNotZeroAddress()
    }
}