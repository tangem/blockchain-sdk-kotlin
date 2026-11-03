package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.parseEthereumAddress
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import java.math.BigInteger

/**
 *  Call data for the method that allows the owner of a specific yield token
 *  to enter the protocol associated with that token.
 *
 *  Signature: `enterProtocolByOwner(address)` when [amount] is null,
 *  `enterProtocolByOwner(address,uint256)` otherwise.
 */
class EthereumYieldSupplyEnterCallData(
    val tokenContractAddress: String,
    val amount: BigInteger? = null,
) : SmartContractCallData {
    override val methodId: String = if (amount == null) METHOD_ID else METHOD_ID_WITH_AMOUNT
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()
            val amountData = amount?.toFixedSizeBytes()

            return if (amountData == null) {
                prefixData + tokenContractAddressData
            } else {
                prefixData + tokenContractAddressData + amountData
            }
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(tokenContractAddress) &&
            tokenContractAddress.isNotZeroAddress() &&
            (amount == null || amount > BigInteger.ZERO)
    }

    companion object {
        const val METHOD_ID = "0x79be55f7"
        const val METHOD_ID_WITH_AMOUNT = "0x3e44dfd2"

        private const val ADDRESS_WORD_HEX_LENGTH = 64
        private const val AMOUNT_WORD_HEX_LENGTH = 64
        private const val HEX_RADIX = 16

        fun decode(rawData: String): EthereumYieldSupplyEnterCallData? {
            return when {
                rawData.startsWith(METHOD_ID_WITH_AMOUNT) -> decodeWithAmount(rawData)
                rawData.startsWith(METHOD_ID) -> decodeWithoutAmount(rawData)
                else -> null
            }
        }

        private fun decodeWithoutAmount(rawData: String): EthereumYieldSupplyEnterCallData? {
            val addressService = EthereumAddressService()
            return runCatching {
                val tokenContractAddress = rawData.removePrefix(METHOD_ID).parseEthereumAddress()
                if (addressService.validate(tokenContractAddress)) {
                    EthereumYieldSupplyEnterCallData(tokenContractAddress = tokenContractAddress)
                } else {
                    null
                }
            }.getOrNull()
        }

        private fun decodeWithAmount(rawData: String): EthereumYieldSupplyEnterCallData? {
            val addressService = EthereumAddressService()
            val payload = rawData.removePrefix(METHOD_ID_WITH_AMOUNT)
            if (payload.length != ADDRESS_WORD_HEX_LENGTH + AMOUNT_WORD_HEX_LENGTH) return null

            val addressWord = payload.substring(0, ADDRESS_WORD_HEX_LENGTH)
            val amountWord = payload.substring(ADDRESS_WORD_HEX_LENGTH)

            return runCatching {
                val tokenContractAddress = addressWord.parseEthereumAddress()
                val amount = BigInteger(amountWord, HEX_RADIX)
                if (addressService.validate(tokenContractAddress)) {
                    EthereumYieldSupplyEnterCallData(tokenContractAddress = tokenContractAddress, amount = amount)
                } else {
                    null
                }
            }.getOrNull()
        }
    }
}