package com.tangem.blockchain.blockchains.tron

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.padLeft
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString

fun createTrc20ApproveDataHex(spender: String, amount: Amount?): String {
    val spenderHex = spender.decodeBase58(checked = true)
        ?.padLeft(TRON_BYTE_ARRAY_PADDING_SIZE) ?: error("wrong spender address")
    val amountHex = amount?.bigIntegerValue()
        ?.toByteArray()
        ?.padLeft(TRON_BYTE_ARRAY_PADDING_SIZE)
        ?: HEX_F.repeat(TRON_ENCODED_BYTE_ARRAY_LENGTH).hexToBytes()
    return (spenderHex + amountHex).toHexString()
}

private const val HEX_F = "f"

internal const val TRON_ENCODED_BYTE_ARRAY_LENGTH = 64
internal const val TRON_BYTE_ARRAY_PADDING_SIZE = 32