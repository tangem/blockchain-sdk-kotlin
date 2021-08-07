package com.tangem.blockchain.extensions

import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.resume


suspend fun <T> retryIO(
        times: Int = 3,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: IOException) {


        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}


//suspend fun <T: Any> handleRequest(requestFunc: suspend () -> T): Result<T> {
//    return try {
//        Result.success(requestFunc.invoke())
//    } catch (he: HttpException) {
//        Result.failure(he)
////        HttpException
////        SocketTimeoutException
////        IOException
//    }
//}


sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: Throwable?) : Result<Nothing>()

    companion object {
        fun failure(sdkError: TangemError): Failure =
                Failure(Exception("TangemError: code: ${sdkError.code}, message: ${sdkError.customMessage}"))
    }
}

sealed class SimpleResult {
    object Success : SimpleResult()
    data class Failure(val error: Throwable?) : SimpleResult()

    fun <T> toResultWithData(data: T): Result<T> {
        return when (this) {
            is Success -> Result.Success(data)
            is Failure -> Result.Failure(this.error)
        }
    }

    companion object {
        fun failure(sdkError: TangemError): Failure =
                Failure(Exception("TangemError: code: ${sdkError.code}, message: ${sdkError.customMessage}"))
    }
}

class Signer(
    private val tangemSdk: TangemSdk, private val initialMessage: Message? = null,
) : TransactionSigner {
    override suspend fun sign(
        hashes: List<ByteArray>, cardId: String, walletPublicKey: ByteArray,
    ): CompletionResult<List<ByteArray>> =
        suspendCancellableCoroutine { continuation ->
            tangemSdk.sign(
                hashes = hashes.toTypedArray(),
                cardId = cardId,
                walletPublicKey = walletPublicKey,
                initialMessage = initialMessage,
            ) { result ->
                when (result) {
                    is CompletionResult.Success ->
                        continuation.resume(CompletionResult.Success(result.data.signatures))
                    is CompletionResult.Failure ->
                        continuation.resume(CompletionResult.Failure(result.error))
                }
            }
        }

    override suspend fun sign(
        hash: ByteArray, cardId: String, walletPublicKey: ByteArray,
    ): CompletionResult<ByteArray> =
        suspendCancellableCoroutine { continuation ->
            tangemSdk.sign(
                hash = hash,
                cardId = cardId,
                walletPublicKey = walletPublicKey,
                initialMessage = initialMessage,
            ) { result ->
                when (result) {
                    is CompletionResult.Success ->
                        continuation.resume(CompletionResult.Success(result.data.signature))
                    is CompletionResult.Failure ->
                        continuation.resume(CompletionResult.Failure(result.error))
                }
            }
        }
}

fun Result<*>.isNetworkError(): Boolean {
    return when (this) {
        is Result.Success -> false
        is Result.Failure -> this.error is IOException || this.error is HttpException
    }
}

fun SimpleResult.isNetworkError(): Boolean {
    return when (this) {
        is SimpleResult.Success -> false
        is SimpleResult.Failure -> this.error is IOException || this.error is HttpException
    }
}

