package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractMethod
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.orZero
import com.tangem.common.extensions.hexToBytes
import org.kethereum.contract.abi.types.leftPadToFixedSize

/**
 * Token transfer smart contract in ERC20 - transfer(address,uint256)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#transfer">EIP-20 Transfer</a>
 */
data class TransferERC20TokenMethod(
    private val destination: String,
    private val amount: Amount,
) : SmartContractMethod {
    override val prefix: String = "0xa9059cbb"
    override val data: ByteArray
        get() {
            val prefixData = prefix.hexToBytes()
            val addressData = destination.hexToBytes().leftPadToFixedSize(fixedSize = 32)
            val amountData = amount.bigIntegerValue().orZero().toByteArray().leftPadToFixedSize(fixedSize = 32)
            return prefixData + addressData + amountData
        }
}