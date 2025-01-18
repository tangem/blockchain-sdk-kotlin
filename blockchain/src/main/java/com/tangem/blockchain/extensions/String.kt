package com.tangem.blockchain.extensions

import com.tangem.blockchain.blockchains.binance.client.encoding.Bech32
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.blockchain.common.HEX_PREFIX
import org.bitcoinj.core.Base58
import java.math.BigDecimal
import java.math.BigInteger

private const val HEX_RADIX = 16

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

fun String?.toBigDecimalOrDefault(default: BigDecimal = BigDecimal.ZERO): BigDecimal =
    this?.toBigDecimalOrNull() ?: default

fun String.isValidHex(): Boolean = this.all { it.isAscii() }

fun String.isSameCase(): Boolean = this.lowercase() == this || this.uppercase() == this

inline fun <R> String?.letNotBlank(block: (String) -> R): R? {
    if (isNullOrBlank()) return null

    return block(this)
}