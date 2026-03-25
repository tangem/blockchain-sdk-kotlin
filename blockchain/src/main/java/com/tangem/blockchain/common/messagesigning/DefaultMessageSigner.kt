package com.tangem.blockchain.common.messagesigning

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result

internal object DefaultMessageSigner : MessageSigner {
    override suspend fun signMessage(
        message: String,
        address: String,
        protocol: String,
        signer: TransactionSigner,
    ): Result<MessageSignatureResult> {
        return Result.Failure(
            BlockchainSdkError.CustomError("Message signing is not supported for this blockchain"),
        )
    }
}