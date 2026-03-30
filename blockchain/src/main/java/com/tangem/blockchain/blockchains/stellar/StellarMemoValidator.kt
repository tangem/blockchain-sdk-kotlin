package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.memo.MemoState
import com.tangem.blockchain.common.memo.MemoValidator
import com.tangem.blockchain.common.memo.isValidUInt64
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr

internal class StellarMemoValidator(
    private val networkProvider: StellarNetworkProvider,
) : MemoValidator {

    override suspend fun isMemoRequired(destinationAddress: String): Result<Boolean> {
        return try {
            val result = networkProvider.checkTargetAccount(destinationAddress, null)
                .successOr { return it }
            Result.Success(result.requiresMemo)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun validateMemo(memo: String): Result<MemoState> {
        if (memo.isEmpty()) return Result.Success(MemoState.Valid)
        val isValid = when {
            memo.all { it.isDigit() } -> {
                memo.isValidUInt64()
            }
            else -> {
                memo.toByteArray().size <= XLM_MEMO_MAX_LENGTH
            }
        }
        return Result.Success(if (isValid) MemoState.Valid else MemoState.Invalid)
    }

    companion object {
        private const val XLM_MEMO_MAX_LENGTH = 28
    }
}