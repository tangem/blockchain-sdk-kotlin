package com.tangem.blockchain.extensions

import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import kotlinx.coroutines.delay
import java.io.IOException

internal suspend fun <T> retryIO(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: IOException) {
            // Do nothing
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: BlockchainError) : Result<Nothing>()

    companion object {
        fun fromTangemSdkError(sdkError: TangemError): Failure =
            Failure(BlockchainSdkError.WrappedTangemError(sdkError))
    }
}

internal inline fun <T> Result<T>.successOr(failureClause: (Result.Failure) -> T): T {
    return when (this) {
        is Result.Success -> this.data
        is Result.Failure -> failureClause(this)
    }
}

internal inline fun <A, B> Result<A>.map(block: (A) -> B): Result<B> {
    return when (this) {
        is Result.Success -> Result.Success(block(this.data))
        is Result.Failure -> this
    }
}

internal fun Result.Failure.toSimpleFailure(): SimpleResult.Failure {
    return SimpleResult.Failure(error)
}

internal fun <T> Result<T>.toSimpleResult(): SimpleResult {
    return when (this) {
        is Result.Success -> SimpleResult.Success
        is Result.Failure -> SimpleResult.Failure(this.error)
    }
}

sealed class SimpleResult {
    object Success : SimpleResult()
    data class Failure(val error: BlockchainError) : SimpleResult()

    companion object {
        fun fromTangemSdkError(sdkError: TangemError): Failure {
            return (sdkError as? BlockchainSdkError)?.let { Failure(it) }
                ?: Failure(BlockchainSdkError.WrappedTangemError(sdkError))
        }
    }
}

internal inline fun SimpleResult.successOr(failureClause: (SimpleResult.Failure) -> Nothing): SimpleResult.Success {
    return when (this) {
        is SimpleResult.Success -> this
        is SimpleResult.Failure -> failureClause(this)
    }
}

internal inline fun <T> CompletionResult<T>.successOr(failureClause: (CompletionResult.Failure<T>) -> Nothing): T {
    return when (this) {
        is CompletionResult.Success -> this.data
        is CompletionResult.Failure -> failureClause(this)
    }
}
