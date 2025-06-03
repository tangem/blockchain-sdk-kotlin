package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * Token allowance call data in ERC20 - allowance(address,address)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#allowance">EIP-20 Allowance</a>
 */
data class AllowanceERC20TokenCallData(
    private val ownerAddress: String,
    private val spenderAddress: String,
) : SmartContractCallData {
    override val methodId = "0xdd62ed3e"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val ownerAddressData = ownerAddress.hexToFixedSizeBytes()
            val spenderAddressData = spenderAddress.hexToFixedSizeBytes()

            return prefixData + ownerAddressData + spenderAddressData
        }
}