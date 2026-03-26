package com.tangem.blockchain.blockchains.xrp

import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.common.memo.MemoState
import com.tangem.blockchain.common.memo.MemoValidator
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result

internal class XrpMemoValidator(
    private val networkProvider: XrpNetworkProvider,
) : MemoValidator {

    override suspend fun isMemoRequired(destinationAddress: String): Result<Boolean> {
        return try {
            val isRequired = networkProvider.checkDestinationTagRequired(destinationAddress)
            Result.Success(isRequired)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun validateMemo(memo: String): Result<MemoState> {
        if (memo.isEmpty()) return Result.Success(MemoState.Valid)
        val tag = memo.toLongOrNull()
        val isValid = tag != null && tag >= 0 && tag <= XRP_TAG_MAX_NUMBER
        return Result.Success(if (isValid) MemoState.Valid else MemoState.Invalid)
    }

    companion object {
        private const val XRP_TAG_MAX_NUMBER = 0xFFFFFFFFL
    }
}