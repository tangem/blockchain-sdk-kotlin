package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.common.smartcontract.SmartContractMethod
import com.tangem.common.extensions.hexToBytes
import org.kethereum.contract.abi.types.leftPadToFixedSize

/**
 * Token balance smart contract in ERC20 - balanceOf(address)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#balanceof">EIP-20 Balance Of</a>
 */
data class TokenBalanceERC20TokenMethod(
    private val address: String,
) : SmartContractMethod {

    override val prefix: String = "0x70a08231"

    override val data: ByteArray
        get() {
            val prefixData = prefix.hexToBytes()
            val addressData = address.hexToBytes().leftPadToFixedSize(fixedSize = 32)
            return prefixData + addressData
        }
}