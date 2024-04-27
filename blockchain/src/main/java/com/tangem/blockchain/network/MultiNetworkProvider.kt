package com.tangem.blockchain.network

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.CycleListIterator
import com.tangem.blockchain.common.ExceptionHandler
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.io.IOException

class MultiNetworkProvider<P : NetworkProvider>(
    val providers: List<P>,
) {
    init {
        if (providers.isEmpty()) error("Empty providers list")
    }

    private val providerIterator = CycleListIterator(providers)
    var currentProvider = providerIterator.next()

    @JvmName("performRawRequest")
    suspend fun <D, R> performRequest(request: suspend P.(D) -> R, data: D): Result<R> =
        RequestWithIOException(request, data).perform()

    suspend fun <D, R> performRequest(request: suspend P.(D) -> Result<R>, data: D): Result<R> =
        DefaultRequest(request, data).perform()

    @JvmName("performSimpleRequest")
    suspend fun <D> performRequest(request: suspend P.(D) -> SimpleResult, data: D): SimpleResult =
        SimpleRequest(request, data).perform()

    suspend fun <R> performRequest(request: suspend P.() -> Result<R>): Result<R> = NoDataRequest(request).perform()

    @JvmName("performRawResultRequest")
    suspend fun <R> performRequest(request: suspend P.() -> R): Result<R> =
        NoDataRequestWithIOException(request).perform()

    private suspend fun <T : Any> Request<P, T>.perform(): T {
        lateinit var finalResult: T

        repeat(providers.size) {
            if (this.lastProvider == currentProvider) currentProvider = providerIterator.next()
            val result = this.performWith(currentProvider)
            if (!isResultNetworkError(result)) {
                return result
            } else {
                val message = "Switchable publisher caught error: ${getErrorMessage(result)}."
                ExceptionHandler.handleApiSwitch(
                    currentHost = currentProvider.baseUrl,
                    nextHost = providerIterator.peekNext().baseUrl,
                    message = message,
                )
            }
            finalResult = result
        }
        return finalResult
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

    private class RequestWithIOException<P, D, R>(
        val request: suspend P.(D) -> R,
        val data: D,
    ) : Request<P, Result<R>>() {

        override suspend fun performWith(provider: P): Result<R> {
            lastProvider = provider
            return try {
                Result.Success(provider.request(data))
            } catch (e: IOException) {
                Result.Failure(BlockchainSdkError.WrappedThrowable(e))
            }
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

    private class NoDataRequestWithIOException<P, R>(
        val request: suspend P.() -> R,
    ) : Request<P, Result<R>>() {

        override suspend fun performWith(provider: P): Result<R> {
            lastProvider = provider
            return try {
                Result.Success(provider.request())
            } catch (e: IOException) {
                Result.Failure(BlockchainSdkError.WrappedThrowable(e))
            }
        }
    }

    private fun <T> isResultNetworkError(result: T): Boolean {
        return when (result) {
            is Result<*> -> ResultChecker.isNetworkError(result = result)
            is SimpleResult -> ResultChecker.isNetworkError(result = result)
            else -> error("Invalid result type")
        }
    }

    private fun <T> getErrorMessage(result: T): String? {
        return when (result) {
            is Result<*> -> ResultChecker.getErrorMessageIfAvailable(result = result)
            is SimpleResult -> ResultChecker.getErrorMessageIfAvailable(result = result)
            else -> null
        }
    }
}
