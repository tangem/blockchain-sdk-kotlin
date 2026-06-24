package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * Call data for partially withdrawing a given [amount] of a yield token from the user's yield module.
 *
 * Signature: `withdraw(address yieldToken, uint256 amount)`
 */
class EthereumYieldSupplyWithdrawCallData(
    val tokenContractAddress: String,
    private val amount: Amount,
) : SmartContractCallData {
    override val methodId: String = METHOD_ID
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()
            val amountData = amount.bigIntegerValue()?.toFixedSizeBytes() ?: error("Invalid withdraw amount")
            return prefixData + tokenContractAddressData + amountData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(tokenContractAddress) && tokenContractAddress.isNotZeroAddress()
    }

    companion object {
        const val METHOD_ID = "0xf3fef3a3"
    }
}