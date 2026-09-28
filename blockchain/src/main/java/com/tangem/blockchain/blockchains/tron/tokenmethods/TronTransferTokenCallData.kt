package com.tangem.blockchain.blockchains.tron.tokenmethods

import com.tangem.blockchain.blockchains.tron.libs.Base58Check
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
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
 * Token transfer call data in TRC20 - transfer(address,uint256)
 *
 * @see <a href="https://developers.tron.network/docs/trc20-contract-interaction#transfer">TRC20 Transfer</a>
 */
class TronTransferTokenCallData(
    val destination: String,
    val amount: Amount,
) : SmartContractCallData {
    override val methodId: String = METHOD_ID
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()

            val addressData = destination.decodeBase58(checked = true)?.padLeft(length = WORD_SIZE)
                ?: byteArrayOf()

            val amountData = amount.bigIntegerValue()?.toByteArray()?.padLeft(length = WORD_SIZE)
                ?: error("Invalid transaction amount")

            return prefixData + addressData + amountData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        val amountValue = amount.bigIntegerValue()
        return destination.isNotEmpty() && amountValue != null && amountValue > java.math.BigInteger.ZERO
    }

    companion object {

        const val METHOD_ID = "0xa9059cbb"

        private const val METHOD_ID_SIZE = 4
        private const val WORD_SIZE = 32
        private const val ARGUMENTS_COUNT = 2
        private const val CALL_DATA_SIZE = METHOD_ID_SIZE + WORD_SIZE * ARGUMENTS_COUNT

        /** A Tron address is the 20-byte hash behind a one-byte prefix, so it occupies 21 bytes of its word. */
        private const val ADDRESS_SIZE = 21
        private const val ADDRESS_PADDING_SIZE = WORD_SIZE - ADDRESS_SIZE
        private const val ADDRESS_PREFIX: Byte = 0x41
        private const val ZERO_BYTE: Byte = 0

        /**
         * Decode a flat TRC20 `transfer(address,uint256)` call data.
         *
         * The [amount] comes back raw, in the token's smallest units, because the call data carries no decimals to
         * scale it by. The [destination] is rendered back in base58, the form the address is read and written in
         * everywhere outside the call data.
         *
         * Returns `null` for call data that is not a TRC20 transfer — including the ERC20 encoding of the same
         * method, which pads the address to 20 bytes and so leaves the Tron prefix byte zero.
         */
        operator fun invoke(compiledData: ByteArray): TronTransferTokenCallData? {
            if (compiledData.size != CALL_DATA_SIZE) return null

            val methodId = compiledData
                .copyOfRange(0, METHOD_ID_SIZE)
                .toHexString()
                .formatHex()

            if (!methodId.equals(METHOD_ID, ignoreCase = true)) return null

            val addressWord = compiledData.copyOfRange(METHOD_ID_SIZE, METHOD_ID_SIZE + WORD_SIZE)
            if (addressWord.take(ADDRESS_PADDING_SIZE).any { it != ZERO_BYTE }) return null

            val addressData = addressWord.copyOfRange(ADDRESS_PADDING_SIZE, WORD_SIZE)
            if (addressData.first() != ADDRESS_PREFIX) return null

            val amountData = compiledData.copyOfRange(METHOD_ID_SIZE + WORD_SIZE, CALL_DATA_SIZE)

            return TronTransferTokenCallData(
                destination = Base58Check.bytesToBase58(addressData),
                amount = Amount(
                    blockchain = Blockchain.Unknown,
                    value = amountData.toBigInteger().toBigDecimal(),
                ),
            )
        }

        /**
         * Decode a flat TRC20 `transfer(address,uint256)` call data from its hex representation, with or without a
         * `0x` prefix. See the [ByteArray] overload for the parsing rules.
         */
        operator fun invoke(compiledData: String): TronTransferTokenCallData? {
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