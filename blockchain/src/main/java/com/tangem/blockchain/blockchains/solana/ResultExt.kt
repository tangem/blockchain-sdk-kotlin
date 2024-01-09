package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult

internal fun <T> Result<T>.toSimpleResult(): SimpleResult {
    return when (this) {
        is Result.Success -> SimpleResult.Success
        is Result.Failure -> SimpleResult.Failure(this.error)
    }
}

internal inline fun <T> CompletionResult<T>.successOr(failureClause: (CompletionResult.Failure<T>) -> Nothing): T {
    return when (this) {
        is CompletionResult.Success -> this.data
        is CompletionResult.Failure -> failureClause(this)
    }
}