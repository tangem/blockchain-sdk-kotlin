package com.tangem.blockchain.yieldlending.providers.ethereum.yield

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import org.kethereum.extensions.toBytesPadded
import java.math.BigInteger

/**
 *  - **initYieldToken(address yieldToken, uint240 maxNetworkFee)**
 *   Signature: `initYieldToken(address,uint240)`
 *   Selector: `0x3b7e4f2f`
 */
class EthereumYieldLendingInitTokenCallData(
    private val tokenContractAddress: String,
    private val maxNetworkFee: Amount,
) : SmartContractCallData {
    override val methodId: String = "0xebd4b81c"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()
            val maxFeeData = maxNetworkFee.bigIntegerValue()?.toBytesPadded(length = 32) ?: error("Invalid fee amount")

            return prefixData + tokenContractAddressData + maxFeeData
        }
}