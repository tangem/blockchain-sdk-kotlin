package com.tangem.blockchain.blockchains.tron.tokenmethods

import com.tangem.blockchain.blockchains.tron.TRON_BYTE_ARRAY_PADDING_SIZE
import com.tangem.blockchain.blockchains.tron.TRON_ENCODED_BYTE_ARRAY_LENGTH
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_F
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchain.extensions.isValidHex
import com.tangem.blockchain.extensions.padLeft
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.kethereum.extensions.toBigInteger

/**
 * Token approval call data in TRC20 - approve(address,uint256)
 *
 * @see <a href="https://developers.tron.network/docs/trc20-contract-interaction#approve">TRC20 Approve</a>
 */
data class TronApprovalTokenCallData(
    val spenderAddress: String,
    val amount: Amount?,
) : SmartContractCallData {
    override val methodId: String = METHOD_ID
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val spenderAddressData = spenderAddress.decodeBase58(checked = true)?.padLeft(TRON_BYTE_ARRAY_PADDING_SIZE)
                ?: error("wrong spender address")

            val unlimitedAmount = HEX_F.repeat(TRON_ENCODED_BYTE_ARRAY_LENGTH).hexToBytes()
            val amountData = amount?.bigIntegerValue()?.toByteArray()?.padLeft(TRON_BYTE_ARRAY_PADDING_SIZE)
                ?: unlimitedAmount

            return prefixData + spenderAddressData + amountData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return spenderAddress.isNotEmpty()
    }

    companion object {

        const val METHOD_ID = "0x095ea7b3"

        private const val METHOD_ID_SIZE = 4
        private const val WORD_SIZE = 32
        private const val ARGUMENTS_COUNT = 2
        private const val CALL_DATA_SIZE = METHOD_ID_SIZE + WORD_SIZE * ARGUMENTS_COUNT

        /**
         * Decode a flat TRC20 `approve(address,uint256)` call data.
         *
         * The [amount] comes back raw, in the token's smallest units, because the call data carries no decimals to
         * scale it by, and `null` for the unlimited allowance the all-ones word stands for. The [spenderAddress] is
         * rendered back in base58 — see [decodeTronAddressWord] for the two encodings it accepts.
         *
         * Returns `null` for call data that is not a TRC20 approval.
         */
        operator fun invoke(compiledData: ByteArray): TronApprovalTokenCallData? {
            if (compiledData.size != CALL_DATA_SIZE) return null

            val methodId = compiledData
                .copyOfRange(0, METHOD_ID_SIZE)
                .toHexString()
                .formatHex()

            if (!methodId.equals(METHOD_ID, ignoreCase = true)) return null

            val spender = compiledData
                .copyOfRange(METHOD_ID_SIZE, METHOD_ID_SIZE + WORD_SIZE)
                .decodeTronAddressWord()
                ?: return null

            val amountData = compiledData.copyOfRange(METHOD_ID_SIZE + WORD_SIZE, CALL_DATA_SIZE)
            val unlimitedAmount = HEX_F.repeat(TRON_ENCODED_BYTE_ARRAY_LENGTH).hexToBytes()

            return TronApprovalTokenCallData(
                spenderAddress = spender,
                amount = if (amountData.contentEquals(unlimitedAmount)) {
                    null
                } else {
                    Amount(blockchain = Blockchain.Unknown, value = amountData.toBigInteger().toBigDecimal())
                },
            )
        }

        /**
         * Decode a flat TRC20 `approve(address,uint256)` call data from its hex representation, with or without a
         * `0x` prefix. See the [ByteArray] overload for the parsing rules.
         */
        operator fun invoke(compiledData: String): TronApprovalTokenCallData? {
            val hex = if (compiledData.startsWith(HEX_PREFIX, ignoreCase = true)) {
                compiledData.substring(HEX_PREFIX.length)
            } else {
                compiledData
            }

            if (!hex.isValidHex()) return null

            return invoke(hex.hexToBytes())
        }
    }
}