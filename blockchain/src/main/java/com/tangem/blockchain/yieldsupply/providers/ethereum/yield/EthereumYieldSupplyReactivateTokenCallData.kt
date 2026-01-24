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
 *  Call data for the method that reactivates a previously deactivated yield token,
 *  allowing users to resume their participation in the associated yield protocol.
 *
 *  Signature: `reactivateToken(address,uint240)`
 */
class EthereumYieldSupplyReactivateTokenCallData(
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

    override fun validate(blockchain: Blockchain): Boolean {
        val feeValue = maxNetworkFee.bigIntegerValue()
        return blockchain.validateAddress(tokenContractAddress) && tokenContractAddress.isNotZeroAddress() &&
            feeValue != null && feeValue > BigInteger.ZERO
    }

    companion object {
        const val METHOD_ID = "0xc478e956"
        fun decode(rawData: String): EthereumYieldSupplyReactivateTokenCallData? {
            if (!rawData.contains(METHOD_ID)) return null

            val addressService = EthereumAddressService()
            return runCatching {
                val dataWithoutMethod = rawData.removePrefix(METHOD_ID)
                val tokenContractAddress = dataWithoutMethod.take(WORD_HEX_LENGTH).parseEthereumAddress()
                if (addressService.validate(tokenContractAddress)) {
                    EthereumYieldSupplyReactivateTokenCallData(
                        tokenContractAddress = tokenContractAddress,
                        maxNetworkFee = Amount(Blockchain.Unknown),
                    )
                } else {
                    null
                }
            }.getOrNull()
        }
    }
}