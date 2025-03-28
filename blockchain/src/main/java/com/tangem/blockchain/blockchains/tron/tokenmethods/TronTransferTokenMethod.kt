package com.tangem.blockchain.blockchains.tron.tokenmethods

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractMethod
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.padLeft
import com.tangem.common.extensions.hexToBytes

/**
 * Token transfer smart contract in TRC20 - transfer(address,uint256)
 *
 * @see <a href="https://developers.tron.network/docs/trc20-contract-interaction#transfer">TRC20 Transfer</a>
 */
class TronTransferTokenMethod(
    val destination: String,
    val amount: Amount,
) : SmartContractMethod {
    override val prefix: String = "0xa9059cbb"
    override val data: ByteArray
        get() {
            val prefixData = prefix.hexToBytes()

            val addressData = destination.decodeBase58(checked = true)?.padLeft(length = 32)
                ?: byteArrayOf()

            val amountData = amount.bigIntegerValue()?.toByteArray()?.padLeft(length = 32)
                ?: byteArrayOf()

            return prefixData + addressData + amountData
        }
}