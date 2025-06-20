package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.HEX_F
import com.tangem.blockchain.common.smartcontract.Erc20CallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import org.kethereum.extensions.toBytesPadded

/**
 * Token approval call data in ERC20 - approve(address,uint256)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#approve">EIP-20 Approve</a>
 */
data class ApprovalERC20TokenCallData(
    private val spenderAddress: String,
    private val amount: Amount?,
) : Erc20CallData {
    override val methodId = "0x095ea7b3"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = spenderAddress.addressWithoutPrefix().hexToFixedSizeBytes()

            val unlimitedAmount = HEX_F.repeat(n = 64).hexToBytes()
            val amountData = amount?.bigIntegerValue()?.toBytesPadded(length = 32) ?: unlimitedAmount
            return prefixData + addressData + amountData
        }
}