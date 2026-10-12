package com.tangem.blockchain.transactionhistory.utils

import com.tangem.blockchain.common.HEX_PREFIX

private const val METHOD_ID_HEX_LENGTH = 8

/** The leading selector of a contract call, read from its raw call data; `null` when there is none to read. */
internal fun methodIdFromRawData(rawData: String?): String? {
    val methodId = rawData?.removePrefix(HEX_PREFIX)?.take(METHOD_ID_HEX_LENGTH)
    return methodId?.takeIf { it.length == METHOD_ID_HEX_LENGTH }
}