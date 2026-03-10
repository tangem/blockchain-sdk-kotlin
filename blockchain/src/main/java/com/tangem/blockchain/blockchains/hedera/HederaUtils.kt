package com.tangem.blockchain.blockchains.hedera

import com.hedera.hashgraph.sdk.TokenId
import com.tangem.Log
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TokenBalanceERC20TokenCallData
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.smartcontract.Erc20CallData
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchain.extensions.hexToBigInteger
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import org.komputing.khex.extensions.toHexString
import java.math.BigInteger

internal object HederaUtils {

    private const val TRANSFER_SELECTOR = "0xa9059cbb"
    private const val EVM_ADDRESS_HEX_LENGTH = 40
    private const val EVM_ADDRESS_FIRST_HALF_LENGTH = 20
    private const val HEX_RADIX = 16

    fun createTokenId(contractAddress: String): TokenId {
        return try {
            TokenId.fromString(contractAddress)
        } catch (_: Exception) {
            try {
                TokenId.fromSolidityAddress(contractAddress)
            } catch (e: Exception) {
                Log.error { e.message.orEmpty() }
                throw BlockchainSdkError.CustomError(e.message.orEmpty())
            }
        }
    }

    /**
     * Convert Hedera account ID (e.g. "0.0.12345") to EVM address format.
     * 1. Remove "0.0." prefix
     * 2. Convert number to hex
     * 3. Pad left to 20 bytes (40 hex chars)
     * 4. Add "0x" prefix
     */
    fun accountIdToEvmAddress(accountId: String): String {
        val num = accountId.removePrefix("0.0.").toLong()
        val hex = num.toString(HEX_RADIX)
        return "0x" + hex.padStart(EVM_ADDRESS_HEX_LENGTH, '0')
    }

    /**
     * Try to convert an EVM address to Hedera account ID format.
     * If first 10 bytes (20 hex chars) are zeros, it's a simple numeric conversion:
     *   - Parse remaining hex as decimal, prepend "0.0."
     * Otherwise returns null (network call needed to resolve).
     */
    fun evmAddressToAccountId(evmAddress: String): String? {
        if (!isValidEvmAddress(evmAddress)) return null

        val hex = evmAddress.removePrefix("0x").removePrefix("0X").lowercase()

        // Check if first 10 bytes (20 hex chars) are zeros
        val firstHalf = hex.substring(0, EVM_ADDRESS_FIRST_HALF_LENGTH)
        if (!firstHalf.all { it == '0' }) return null
        val num = hex.substring(EVM_ADDRESS_FIRST_HALF_LENGTH).hexToBigInteger()
        return "0.0.$num"
    }

    fun isValidEvmAddress(address: String): Boolean {
        val hex = address.removePrefix("0x").removePrefix("0X")
        return hex.length == EVM_ADDRESS_HEX_LENGTH && hex.all { it.lowercaseChar() in "0123456789abcdef" }
    }

    /**
     * Encode ERC20 balanceOf(address) call data.
     * Function selector: 0x70a08231
     * Parameter: address padded to 32 bytes
     */
    fun encodeBalanceOf(ownerEvmAddress: String): String {
        return TokenBalanceERC20TokenCallData(ownerEvmAddress).dataHex.formatHex()
    }

    /**
     * Encode ERC20 transfer(address,uint256) call data.
     * Function selector: 0xa9059cbb
     * Parameters: address padded to 32 bytes + amount padded to 32 bytes
     */
    fun encodeTransfer(toEvmAddress: String, amount: BigInteger): String {
        val selector = TRANSFER_SELECTOR.hexToBytes()
        val addressData = Erc20CallData.addressWithoutPrefix(toEvmAddress).hexToFixedSizeBytes()
        val amountData = amount.toFixedSizeBytes()
        return (selector + addressData + amountData).toHexString().formatHex()
    }
}