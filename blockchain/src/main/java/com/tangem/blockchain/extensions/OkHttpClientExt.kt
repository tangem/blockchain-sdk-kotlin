package com.tangem.blockchain.extensions

import okhttp3.OkHttpClient

internal fun OkHttpClient.Builder.addHeaders(headers: Map<String, String>): OkHttpClient.Builder {
    return if (headers.isNotEmpty()) {
        addInterceptor(interceptor = AddHeaderInterceptor(headers))
    } else {
        this
    }
}
