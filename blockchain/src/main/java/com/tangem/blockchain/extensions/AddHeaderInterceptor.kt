package com.tangem.blockchain.extensions

import okhttp3.Interceptor
import okhttp3.Response

class AddHeaderInterceptor(private val headers: Map<String, String>) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            headers.forEach {
                addHeader(it.key, it.value)
            }
        }.build()

        return chain.proceed(request)
    }
}
