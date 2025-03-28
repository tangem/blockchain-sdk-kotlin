package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.HEX_F
import com.tangem.blockchain.common.smartcontract.SmartContractMethod
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.common.extensions.hexToBytes
import org.kethereum.extensions.toBytesPadded
import org.kethereum.extensions.toFixedLengthByteArray

/**
 * Token approval smart contract in ERC20 - approve(address,uint256)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#approve">EIP-20 Approve</a>
 */
data class ApprovalERC20TokenMethod(
    private val spenderAddress: String,
    private val amount: Amount?,
) : SmartContractMethod {
    override val prefix = "0x095ea7b3"
    override val data: ByteArray
        get() {
            val prefixData = prefix.hexToBytes()
            val addressData = spenderAddress.substring(2).hexToBytes().toFixedLengthByteArray(fixedSize = 32)

            val unlimitedAmount = HEX_F.repeat(n = 64).hexToBytes()
            val amountData = amount?.bigIntegerValue()?.toBytesPadded(length = 32) ?: unlimitedAmount
            return prefixData + addressData + amountData
        }
}