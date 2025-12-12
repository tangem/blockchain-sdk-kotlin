package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for the method that reactivates a previously deactivated yield token,
 *  allowing users to resume their participation in the associated yield protocol.
 *
 *  Signature: `send(address,address,uint)`
 */
class EthereumYieldSupplySendCallData(
    private val tokenContractAddress: String,
    val destinationAddress: String,
    private val amount: Amount,
) : SmartContractCallData {
    override val methodId: String = "0x0779afe6"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()
            val destinationAddressData = destinationAddress.hexToFixedSizeBytes()
            val amountData = amount.bigIntegerValue()?.toFixedSizeBytes() ?: error("Invalid token transfer amount")

            return prefixData + tokenContractAddressData + destinationAddressData + amountData
        }
}