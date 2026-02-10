package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_F
import com.tangem.blockchain.common.smartcontract.Erc20CallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.kethereum.extensions.toBigInteger
import org.kethereum.extensions.toBytesPadded

/**
 * Token approval call data in ERC20 - approve(address,uint256)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#approve">EIP-20 Approve</a>
 */
data class ApprovalERC20TokenCallData(
    val spenderAddress: String,
    private val amount: Amount?,
) : Erc20CallData {
    override val methodId = APPROVE_METHOD_ID
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = spenderAddress.addressWithoutPrefix().hexToFixedSizeBytes()

            val amountData = amount?.bigIntegerValue()?.toBytesPadded(length = 32) ?: unlimitedAmount
            return prefixData + addressData + amountData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(spenderAddress) && spenderAddress.isNotZeroAddress()
    }

    companion object {

        private val unlimitedAmount = HEX_F.repeat(n = 64).hexToBytes()
        private const val METHOD_ID_SIZE = 4
        private const val WORD_SIZE = 32
        private const val APPROVE_METHOD_ID = "0x095ea7b3"

        operator fun invoke(compiledData: ByteArray): ApprovalERC20TokenCallData? {
            if (compiledData.size < METHOD_ID_SIZE + WORD_SIZE * 2) return null

            val methodId = compiledData
                .copyOfRange(0, METHOD_ID_SIZE)
                .toHexString()
                .formatHex()

            if (!methodId.equals(APPROVE_METHOD_ID, ignoreCase = true)) return null

            val addressData = compiledData.copyOfRange(
                METHOD_ID_SIZE,
                METHOD_ID_SIZE + WORD_SIZE,
            )
            val amountData = compiledData.copyOfRange(
                METHOD_ID_SIZE + WORD_SIZE,
                METHOD_ID_SIZE + WORD_SIZE * 2,
            )
            val spender = Erc20CallData.addressWithoutPrefix(addressData.toHexString()).formatHex()

            val amount = if (amountData.contentEquals(unlimitedAmount)) {
                null
            } else {
                Amount(
                    blockchain = Blockchain.Unknown,
                    value = amountData.toBigInteger().toBigDecimal(),
                )
            }

            return ApprovalERC20TokenCallData(
                spenderAddress = spender,
                amount = amount,
            )
        }
    }
}