package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.ADDRESS_HEX_LENGTH
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.Erc20CallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * Token transfer call data in ERC20 - transfer(address,uint256)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#transfer">EIP-20 Transfer</a>
 */
data class TransferERC20TokenCallData(private val destination: String, private val amount: Amount) : Erc20CallData {
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

    private companion object {

        const val METHOD_ID = "0xa9059cbb"

        const val ZERO_BYTE: Byte = 0
        const val HEX_RADIX = 16
    }
}