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
 *  Call data for the swap function of the Yield Module V2 contract.
 *  Allows swapping tokens held in the yield module via a DEX provider.
 *
 *  Signature: `swap(address tokenIn, uint256 amountIn, address target, address spender, bytes data)`
 */
@Suppress("MagicNumber")
class EthereumYieldSupplySwapCallData(
    private val tokenIn: String,
    private val amountIn: BigInteger,
    private val target: String,
    private val spender: String,
    private val swapData: ByteArray,
) : SmartContractCallData {

    override val methodId: String = METHOD_ID

    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()

            val tokenInData = tokenIn.hexToFixedSizeBytes()
            val amountInData = amountIn.toFixedSizeBytes()
            val targetData = target.hexToFixedSizeBytes()
            val spenderData = spender.hexToFixedSizeBytes()

            // Offset to dynamic bytes parameter (5 static slots * 32 bytes = 160 = 0xa0)
            val dataOffset = BigInteger.valueOf(DYNAMIC_OFFSET).toFixedSizeBytes()

            // Dynamic bytes: length + padded data
            val dataLength = BigInteger.valueOf(swapData.size.toLong()).toFixedSizeBytes()
            val paddedData = if (swapData.isEmpty()) {
                byteArrayOf()
            } else {
                swapData.toFixedSizeBytesRightPadding(
                    fixedSize = ((swapData.size + 31) / 32) * 32,
                )
            }

            return prefixData +
                tokenInData +
                amountInData +
                targetData +
                spenderData +
                dataOffset +
                dataLength +
                paddedData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(tokenIn) && tokenIn.isNotZeroAddress() &&
            blockchain.validateAddress(target) && target.isNotZeroAddress() &&
            blockchain.validateAddress(spender) && spender.isNotZeroAddress() &&
            amountIn > BigInteger.ZERO
    }

    companion object {
        const val METHOD_ID = "0x4c3f521d"
        private const val DYNAMIC_OFFSET = 160L // 5 * 32 bytes
    }
}