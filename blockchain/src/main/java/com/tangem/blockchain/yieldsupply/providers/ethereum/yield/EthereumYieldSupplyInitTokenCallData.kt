package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.WORD_HEX_LENGTH
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.parseEthereumAddress
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import org.kethereum.extensions.toBytesPadded
import java.math.BigInteger

/**
 *  Call data for the method that initializes a yield token within the yield module protocol
 *
 *  Signature: `initYieldToken(address,uint240)`
 */
class EthereumYieldSupplyInitTokenCallData(
    val tokenContractAddress: String,
    private val maxNetworkFee: Amount,
) : SmartContractCallData {
    override val methodId: String = METHOD_ID
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()
            val maxFeeData = maxNetworkFee.bigIntegerValue()?.toBytesPadded(length = 32) ?: error("Invalid fee amount")

            return prefixData + tokenContractAddressData + maxFeeData
        }

    override fun validate(): Boolean {
        val feeValue = maxNetworkFee.bigIntegerValue()
        return EthereumAddressService().validate(tokenContractAddress) && tokenContractAddress.isNotZeroAddress() &&
            feeValue != null && feeValue > BigInteger.ZERO
    }

    companion object {
        const val METHOD_ID = "0xebd4b81c"
        fun decode(rawData: String): EthereumYieldSupplyInitTokenCallData? {
            if (!rawData.contains(METHOD_ID)) return null

            val addressService = EthereumAddressService()
            return runCatching {
                val dataWithoutMethod = rawData.removePrefix(METHOD_ID)
                val tokenContractAddress = dataWithoutMethod.take(WORD_HEX_LENGTH).parseEthereumAddress()

                if (addressService.validate(tokenContractAddress)) {
                    EthereumYieldSupplyInitTokenCallData(
                        tokenContractAddress = tokenContractAddress,
                        maxNetworkFee = Amount(Blockchain.Unknown), // !!!WARNING ignore max fee data for now
                    )
                } else {
                    null
                }
            }.getOrNull()
        }
    }
}