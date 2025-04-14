package com.tangem.blockchain.extensions

import retrofit2.HttpException

fun HttpException.isApiKeyNeeded(currentApiKey: String?, apiKey: String?): Boolean {
    return apiLimitErrorCodes.contains(code()) && currentApiKey == null && apiKey != null
}

private val apiLimitErrorCodes: List<Int>
    get() = listOf(
        402,
        429,
        430,
        434,
        503, // https://blockchair.com/api/docs#link_M05
    )
