package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
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
    private val newImplementation: String,
    private val callData: ByteArray,
) : SmartContractCallData {

    override val methodId: String = METHOD_ID

    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()

            val implementationData = newImplementation.hexToFixedSizeBytes()

            // Offset to dynamic bytes parameter (2 static slots * 32 bytes = 64 = 0x40)
            val dataOffset = BigInteger.valueOf(DYNAMIC_OFFSET).toFixedSizeBytes()

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
        return blockchain.validateAddress(newImplementation) && newImplementation.isNotZeroAddress()
    }

    companion object {
        const val METHOD_ID = "0x4f1ef286"
        private const val DYNAMIC_OFFSET = 64L // 2 * 32 bytes
    }
}