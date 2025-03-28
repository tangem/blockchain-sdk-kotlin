package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.common.smartcontract.SmartContractMethod
import com.tangem.common.extensions.hexToBytes
import org.kethereum.extensions.toFixedLengthByteArray

/**
 * Token allowance smart contract in ERC20 - allowance(address,address)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#allowance">EIP-20 Allowance</a>
 */
data class AllowanceERC20TokenMethod(
    private val ownerAddress: String,
    private val spenderAddress: String,
) : SmartContractMethod {
    override val prefix = "0xdd62ed3e"
    override val data: ByteArray
        get() {
            val prefixData = prefix.hexToBytes()
            val ownerAddressData = ownerAddress.substring(2).hexToBytes().toFixedLengthByteArray(fixedSize = 32)
            val spenderAddressData = spenderAddress.substring(2).hexToBytes().toFixedLengthByteArray(fixedSize = 32)

            return prefixData + ownerAddressData + spenderAddressData
        }
}