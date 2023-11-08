package com.tangem.blockchain.extensions

import com.tangem.blockchain.blockchains.binance.client.encoding.Bech32
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import org.bitcoinj.core.Base58
import java.math.BigDecimal

fun String.decodeBase58(checked: Boolean = false): ByteArray? {
    return try {
        if (checked) Base58.decodeChecked(this) else Base58.decode(this)
    } catch (exception: Exception) {
        null
    }
}

fun String.decodeBech32(): ByteArray? {
    return try {
        val decoded: ByteArray = Bech32.decode(this).data
        Crypto.convertBits(decoded, 0, decoded.size, 5, 8, false)
    } catch (exception: Exception) {
        null
    }
}

fun String.hexToBigDecimal(): BigDecimal? {
    return removePrefix("0x").toBigIntegerOrNull(16)?.toBigDecimal()
}

fun String?.toBigDecimalOrDefault(default: BigDecimal = BigDecimal.ZERO): BigDecimal =
    this?.toBigDecimalOrNull() ?: default

fun String.isValidHex(): Boolean = this.all { it.isAscii() }

inline fun <R> String?.letNotBlank(block: (String) -> R): R? {
    if (isNullOrBlank()) return null

    return block(this)
}
