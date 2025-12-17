package com.tangem.blockchain.common.psbt

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result

internal object DefaultPsbtProvider : PsbtProvider {
    override suspend fun signPsbt(
        psbtBase64: String,
        signInputs: Any,
        signer: TransactionSigner,
    ): Result<String> {
        return Result.Failure(
            BlockchainSdkError.CustomError("PSBT signing is not supported for this blockchain"),
        )
    }

    override suspend fun broadcastPsbt(psbtBase64: String): Result<String> {
        return Result.Failure(
            BlockchainSdkError.CustomError("PSBT broadcasting is not supported for this blockchain"),
        )
    }
}