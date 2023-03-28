package com.tangem.blockchain.network

import com.tangem.blockchain.common.CycleListIterator
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.isNetworkError

class MultiNetworkProvider<P>(val providers: List<P>) {
    init {
        if (providers.isEmpty()) throw Exception("Empty providers list")
    }

    private val providerIterator = CycleListIterator(providers)
    var currentProvider = providerIterator.next()

    suspend fun <D, R> performRequest(request: suspend P.(D) -> Result<R>, data: D): Result<R> =
        DefaultRequest(request, data).perform()

    @JvmName("performSimpleRequest")
    suspend fun <D> performRequest(request: suspend P.(D) -> SimpleResult, data: D): SimpleResult =
        SimpleRequest(request, data).perform()

    suspend fun <R> performRequest(request: suspend P.() -> Result<R>): Result<R> =
        NoDataRequest(request).perform()

    private suspend fun <T> Request<P, T>.perform(): T {
        var result: T? = null

        repeat(providers.size) {
            if (this.lastProvider == currentProvider) currentProvider = providerIterator.next()
            result = this.performWith(currentProvider)
            if (!result!!.isResultNetworkError()) return result!!
        }
        return result!!
    }

    private abstract class Request<P, T> {
        var lastProvider: P? = null

        abstract suspend fun performWith(provider: P): T
    }

    private class DefaultRequest<P, D, R>(
        val request: suspend P.(D) -> Result<R>,
        val data: D,
    ) : Request<P, Result<R>>() {

        override suspend fun performWith(provider: P): Result<R> {
            lastProvider = provider
            return provider.request(data)
        }
    }

    private class SimpleRequest<P, D>(
        val request: suspend P.(D) -> SimpleResult,
        val data: D,
    ) : Request<P, SimpleResult>() {

        override suspend fun performWith(provider: P): SimpleResult {
            lastProvider = provider
            return provider.request(data)
        }
    }

    private class NoDataRequest<P, R>(
        val request: suspend P.() -> Result<R>,
    ) : Request<P, Result<R>>() {

        override suspend fun performWith(provider: P): Result<R> {
            lastProvider = provider
            return provider.request()
        }
    }

    private fun Any.isResultNetworkError(): Boolean {
        return when (this) {
            is Result<*> -> this.isNetworkError()
            is SimpleResult -> this.isNetworkError()
            else -> throw Exception("Invalid result type")
        }
    }
}