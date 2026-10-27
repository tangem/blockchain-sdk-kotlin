package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.ADDRESS_HEX_LENGTH
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.HEX_CHARS_PER_BYTE
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.WORD_HEX_LENGTH
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytesRightPadding
import com.tangem.common.extensions.hexToBytes
import java.math.BigInteger

/**
 *  Call data for the upgradeToAndCall function of the Yield Module contract.
 *  Upgrades the module to a new implementation and calls a function in a single transaction.
 *
 *  Signature: `upgradeToAndCall(address newImplementation, bytes memory data)`
 */
@Suppress("MagicNumber")
class EthereumYieldSupplyUpgradeToAndCallCallData(
    val newImplementation: String,
    val innerCallData: SmartContractCallData,
) : SmartContractCallData {

    override val methodId: String = METHOD_ID

    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()

            val implementationData = newImplementation.hexToFixedSizeBytes()

            // Offset to dynamic bytes parameter (2 static slots * 32 bytes = 64 = 0x40)
            val dataOffset = BigInteger.valueOf(DYNAMIC_OFFSET).toFixedSizeBytes()

            val callData = innerCallData.data
            val dataLength = BigInteger.valueOf(callData.size.toLong()).toFixedSizeBytes()
            val paddedData = if (callData.isEmpty()) {
                byteArrayOf()
            } else {
                callData.toFixedSizeBytesRightPadding(fixedSize = (callData.size + 31) / 32 * 32)
            }

            return prefixData +
                implementationData +
                dataOffset +
                dataLength +
                paddedData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(newImplementation) && newImplementation.isNotZeroAddress() &&
            innerCallData.validate(blockchain)
    }

    companion object {
        const val METHOD_ID = "0x4f1ef286"
        private const val DYNAMIC_OFFSET = 64L // 2 * 32 bytes
        private const val METHOD_ID_HEX_LENGTH = 8
        private val MAX_INT = BigInteger.valueOf(Int.MAX_VALUE.toLong())

        /**
         * Decodes a raw `upgradeToAndCall` call. The inner call keeps only its bytes, so it comes back as
         * [CompiledSmartContractCallData].
         */
        fun decode(rawData: String): EthereumYieldSupplyUpgradeToAndCallCallData? {
            if (!rawData.startsWith(METHOD_ID, ignoreCase = true)) return null

            val addressService = EthereumAddressService()
            return runCatching {
                val arguments = rawData.substring(METHOD_ID.length)
                val newImplementation = arguments.wordAt(index = 0).takeLast(ADDRESS_HEX_LENGTH).formatHex().lowercase()
                val dataOffset = arguments.wordAt(index = 1).toBigInteger(radix = 16)
                val dataLengthValue = arguments.wordAt(index = 2).toBigInteger(radix = 16)
                require(dataLengthValue <= MAX_INT) { "Inner call length overflows int" }
                val dataLength = dataLengthValue.toInt()
                val dataStart = WORD_HEX_LENGTH * 3
                val innerHex = arguments.substring(dataStart, dataStart + dataLength * HEX_CHARS_PER_BYTE)

                val isValid = addressService.validate(newImplementation) &&
                    dataOffset == BigInteger.valueOf(DYNAMIC_OFFSET) &&
                    innerHex.length >= METHOD_ID_HEX_LENGTH
                if (isValid) {
                    EthereumYieldSupplyUpgradeToAndCallCallData(
                        newImplementation = newImplementation,
                        innerCallData = CompiledSmartContractCallData(innerHex.hexToBytes()),
                    )
                } else {
                    null
                }
            }.getOrNull()
        }

        private fun String.wordAt(index: Int): String =
            substring(index * WORD_HEX_LENGTH, (index + 1) * WORD_HEX_LENGTH)
    }
}

fun SmartContractCallData.unwrapYieldModuleUpgrade(): SmartContractCallData =
    (this as? EthereumYieldSupplyUpgradeToAndCallCallData)?.innerCallData ?: this

fun TransactionData.unwrapYieldModuleUpgrade(): TransactionData {
    val uncompiled = this as? TransactionData.Uncompiled ?: return this
    val extras = uncompiled.extras as? EthereumTransactionExtras ?: return this
    val callData = extras.callData ?: return this
    val unwrapped = callData.unwrapYieldModuleUpgrade()
    if (unwrapped === callData) return this

    return uncompiled.copy(extras = extras.copy(callData = unwrapped))
}