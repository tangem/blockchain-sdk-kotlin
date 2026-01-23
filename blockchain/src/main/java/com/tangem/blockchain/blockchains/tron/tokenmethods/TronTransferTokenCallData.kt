package com.tangem.blockchain.blockchains.tron.tokenmethods

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.padLeft
import com.tangem.common.extensions.hexToBytes

/**
 * Token transfer call data in TRC20 - transfer(address,uint256)
 *
 * @see <a href="https://developers.tron.network/docs/trc20-contract-interaction#transfer">TRC20 Transfer</a>
 */
class TronTransferTokenCallData(
    val destination: String,
    val amount: Amount,
) : SmartContractCallData {
    override val methodId: String = "0xa9059cbb"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()

            val addressData = destination.decodeBase58(checked = true)?.padLeft(length = 32)
                ?: byteArrayOf()

            val amountData = amount.bigIntegerValue()?.toByteArray()?.padLeft(length = 32)
                ?: error("Invalid transaction amount")

            return prefixData + addressData + amountData
        }

    override fun validate(): Boolean {
        val amountValue = amount.bigIntegerValue()
        return destination.isNotEmpty() && amountValue != null && amountValue > java.math.BigInteger.ZERO
    }
}