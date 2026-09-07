package com.tangem.blockchain.extensions

import com.tangem.blockchain.blockchains.binance.client.encoding.Bech32
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.common.extensions.hexToBytes
import org.bitcoinj.core.Base58
import org.kethereum.contract.abi.types.leftPadToFixedSize
import org.kethereum.contract.abi.types.rightPadToFixedSize
import java.math.BigDecimal
import java.math.BigInteger

private const val HEX_RADIX = 16
private const val HEX_CHARS_PER_BYTE = 2

fun String.decodeBase58(checked: Boolean = false): ByteArray? {
    return try {
        if (checked) Base58.decodeChecked(this) else Base58.decode(this)
    } catch (exception: Exception) {
        null
    }
}

@Suppress("MagicNumber")
fun String.decodeBech32(): ByteArray? {
    return try {
        val decoded: ByteArray = Bech32.decode(this).data
        Crypto.convertBits(decoded, 0, decoded.size, 5, 8, false)
    } catch (exception: Exception) {
        null
    }
}

fun String.replaceLast(oldValue: String, newValue: String, ignoreCase: Boolean = false): String {
    val index = lastIndexOf(oldValue, ignoreCase = ignoreCase)
    return if (index < 0) this else this.replaceRange(index, index + oldValue.length, newValue)
}

fun String.hexToBigDecimal(default: BigDecimal = BigDecimal.ZERO): BigDecimal {
    return removePrefix(HEX_PREFIX).toBigIntegerOrNull(HEX_RADIX)?.toBigDecimal() ?: default
}

fun String.hexToBigInteger(default: BigInteger = BigInteger.ZERO): BigInteger {
    return removePrefix(HEX_PREFIX).toBigIntegerOrNull(radix = HEX_RADIX) ?: default
}

fun String.hexToInt(default: Int = 0): Int {
    return removePrefix(HEX_PREFIX).toIntOrNull(radix = HEX_RADIX) ?: default
}

fun String.formatHex(): String {
    return if (this.startsWith(HEX_PREFIX)) this else HEX_PREFIX.plus(this)
}

fun String?.toBigDecimalOrDefault(default: BigDecimal = BigDecimal.ZERO): BigDecimal =
    this?.toBigDecimalOrNull() ?: default

/**
 * Checks that the string is a hex encoding of a whole number of bytes: hex digits only, even length.
 *
 * Both conditions matter for callers that decode the string afterwards — `hexToBytes` cannot turn an odd-length or
 * non-hex string into a [ByteArray], so accepting one here only defers the failure.
 */
fun String.isValidHex(): Boolean = length % HEX_CHARS_PER_BYTE == 0 && all(Char::isHexDigit)

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

fun String.isSameCase(): Boolean = this.lowercase() == this || this.uppercase() == this

inline fun <R> String?.letNotBlank(block: (String) -> R): R? {
    if (isNullOrBlank()) return null

    return block(this)
}

fun String.hexToFixedSizeBytes(fixedSize: Int = 32) = hexToBytes().leftPadToFixedSize(fixedSize = fixedSize)

fun ByteArray.toFixedSizeBytes(fixedSize: Int = 32) = leftPadToFixedSize(fixedSize = fixedSize)

fun ByteArray.toFixedSizeBytesRightPadding(fixedSize: Int = 32) = rightPadToFixedSize(fixedSize = fixedSize)