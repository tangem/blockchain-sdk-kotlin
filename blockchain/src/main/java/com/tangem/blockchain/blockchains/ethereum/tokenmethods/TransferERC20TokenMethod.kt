package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.common.smartcontract.SmartContractMethod
import com.tangem.common.extensions.hexToBytes
import org.kethereum.contract.abi.types.leftPadToFixedSize
import java.math.BigInteger

data class TransferERC20TokenMethod(
    private val destination: String,
    private val amount: BigInteger,
) : SmartContractMethod {
    override val prefix: String = "0xa9059cbb"
    override val data: ByteArray
        get() {
            val prefixData = prefix.hexToBytes()
            val addressData = destination.hexToBytes().leftPadToFixedSize(fixedSize = 32)
            val amountData = amount.toByteArray().leftPadToFixedSize(fixedSize = 32)
            return prefixData + addressData + amountData
        }
}
