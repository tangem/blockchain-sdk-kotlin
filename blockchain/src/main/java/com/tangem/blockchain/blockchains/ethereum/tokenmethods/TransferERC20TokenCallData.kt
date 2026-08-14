package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.ADDRESS_HEX_LENGTH
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.smartcontract.Erc20CallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.kethereum.extensions.toBigInteger

/**
 * Token transfer call data in ERC20 - transfer(address,uint256)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#transfer">EIP-20 Transfer</a>
 */
data class TransferERC20TokenCallData(val destination: String, val amount: Amount) : Erc20CallData {
    override val methodId: String = METHOD_ID
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = encodeDestination()
            val amountData = amount.bigIntegerValue()?.toFixedSizeBytes()
                ?: error("Invalid token transfer amount")
            return prefixData + addressData + amountData
        }

    /**
     * `hexToFixedSizeBytes` left-pads to 32 bytes, so a blank or truncated [destination] would be encoded as a burn
     * address. The check lives here rather than only in [validate] because the gasless meta-transaction path never
     * reaches [com.tangem.blockchain.common.TransactionValidator].
     */
    private fun encodeDestination(): ByteArray {
        val addressHex = destination.addressWithoutPrefix()
        require(addressHex.length == ADDRESS_HEX_LENGTH && addressHex.all { it.digitToIntOrNull(HEX_RADIX) != null }) {
            "Invalid ERC20 transfer destination"
        }

        return addressHex.hexToFixedSizeBytes().also { addressData ->
            require(addressData.any { it != ZERO_BYTE }) { "ERC20 transfer to the zero address" }
        }
    }

    override fun validate(blockchain: Blockchain): Boolean {
        val amountValue = amount.bigIntegerValue()
        val isValidAddress = blockchain.validateAddress(destination) && destination.isNotZeroAddress()
        return isValidAddress && amountValue != null
    }

    companion object {

        const val METHOD_ID = "0xa9059cbb"

        private const val HEX_RADIX = 16
        private const val METHOD_ID_SIZE = 4
        private const val WORD_SIZE = 32
        private const val ARGUMENTS_COUNT = 2
        private const val CALL_DATA_SIZE = METHOD_ID_SIZE + WORD_SIZE * ARGUMENTS_COUNT
        private const val ADDRESS_SIZE = 20
        private const val ADDRESS_PADDING_SIZE = WORD_SIZE - ADDRESS_SIZE
        private const val ZERO_BYTE: Byte = 0

        /**
         * Decode a flat ERC20 `transfer(address,uint256)` call data.
         *
         */
        operator fun invoke(compiledData: ByteArray): TransferERC20TokenCallData? {
            if (compiledData.size != CALL_DATA_SIZE) return null

            val methodId = compiledData
                .copyOfRange(0, METHOD_ID_SIZE)
                .toHexString()
                .formatHex()

            if (!methodId.equals(METHOD_ID, ignoreCase = true)) return null

            val addressData = compiledData.copyOfRange(
                METHOD_ID_SIZE,
                METHOD_ID_SIZE + WORD_SIZE,
            )
            if (addressData.take(ADDRESS_PADDING_SIZE).any { it != ZERO_BYTE }) return null

            val destination = Erc20CallData.addressWithoutPrefix(addressData.toHexString()).formatHex().lowercase()
            if (!destination.isNotZeroAddress()) return null

            val amountData = compiledData.copyOfRange(
                METHOD_ID_SIZE + WORD_SIZE,
                CALL_DATA_SIZE,
            )

            return TransferERC20TokenCallData(
                destination = destination,
                amount = Amount(
                    blockchain = Blockchain.Unknown,
                    value = amountData.toBigInteger().toBigDecimal(),
                ),
            )
        }

        /**
         * Decode a flat ERC20 `transfer(address,uint256)` call data from its hex representation.
         *
         * Accepts the string with a `0x` prefix, with a `0X` prefix and without any prefix. Invalid hex yields
         * `null`. See the [ByteArray] overload for the parsing rules and for the semantics of the decoded amount.
         */
        operator fun invoke(compiledData: String): TransferERC20TokenCallData? {
            val hex = if (compiledData.startsWith(HEX_PREFIX, ignoreCase = true)) {
                compiledData.substring(HEX_PREFIX.length)
            } else {
                compiledData
            }

            if (hex.length % 2 != 0 || !hex.all { it.isHexDigit() }) return null

            return invoke(hex.hexToBytes())
        }

        private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}