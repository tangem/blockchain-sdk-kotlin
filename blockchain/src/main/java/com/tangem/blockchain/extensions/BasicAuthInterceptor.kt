package com.tangem.blockchain.extensions

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class BasicAuthInterceptor(
    private val username: String,
    private val password: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("Authorization", Credentials.basic(username, password))
            .build()

        return chain.proceed(request)
    }
}
