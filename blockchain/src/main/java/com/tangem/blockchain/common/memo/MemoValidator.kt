package com.tangem.blockchain.common.memo

import com.tangem.blockchain.extensions.Result

interface MemoValidator {

    /**
     * Checks if memo/destination tag is required for the given [destinationAddress].
     * Returns false by default for blockchains that don't support memo.
     */
    suspend fun isMemoRequired(destinationAddress: String): Result<Boolean>

    /**
     * Validates memo format for the blockchain.
     * Returns MemoState.NotSupported for blockchains without memo.
     */
    suspend fun validateMemo(memo: String): Result<MemoState>
}

object DefaultMemoValidator : MemoValidator {

    override suspend fun isMemoRequired(destinationAddress: String): Result<Boolean> {
        return Result.Success(false)
    }

    override suspend fun validateMemo(memo: String): Result<MemoState> {
        return Result.Success(MemoState.NotSupported)
    }
}