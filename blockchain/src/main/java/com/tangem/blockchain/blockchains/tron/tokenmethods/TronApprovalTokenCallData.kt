package com.tangem.blockchain.blockchains.tron.tokenmethods

import com.tangem.blockchain.blockchains.tron.TRON_BYTE_ARRAY_PADDING_SIZE
import com.tangem.blockchain.blockchains.tron.TRON_ENCODED_BYTE_ARRAY_LENGTH
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.HEX_F
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.padLeft
import com.tangem.common.extensions.hexToBytes

/**
 * Token approval call data in TRC20 - approve(address,uint256)
 *
 * @see <a href="https://developers.tron.network/docs/trc20-contract-interaction#approve">TRC20 Approve</a>
 */
data class TronApprovalTokenCallData(
    private val spenderAddress: String,
    private val amount: Amount?,
) : SmartContractCallData {
    override val methodId: String = "0x095ea7b3"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val spenderAddressData = spenderAddress.decodeBase58(checked = true)?.padLeft(TRON_BYTE_ARRAY_PADDING_SIZE)
                ?: error("wrong spender address")

            val unlimitedAmount = HEX_F.repeat(TRON_ENCODED_BYTE_ARRAY_LENGTH).hexToBytes()
            val amountData = amount?.bigIntegerValue()?.toByteArray()?.padLeft(TRON_BYTE_ARRAY_PADDING_SIZE)
                ?: unlimitedAmount

            return prefixData + spenderAddressData + amountData
        }
}