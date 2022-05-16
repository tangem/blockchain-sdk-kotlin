package com.tangem.blockchain.common

import com.tangem.common.core.TangemError


sealed class BlockchainSdkError(
    override val code: Int = 0,
    override var customMessage: String = code.toString(),
    override val messageResId: Int? = null
) : TangemError, Exception() {

    class NPError(forValue: String) : BlockchainSdkError(-1, "$forValue must be not NULL")
    class UnsupportedOperation(message: String = "Unsupported operation") : BlockchainSdkError(0, message)

    class SendException(val blockchain: Blockchain, message: String) : BlockchainSdkError(1, message)
    object AccountNotFound : BlockchainSdkError(2, "Account not found")
    class CreateAccountUnderfunded(val minReserve: Amount) : BlockchainSdkError(3)
    object FailedToLoadFee : BlockchainSdkError(4, "Failed to load fee")

    object SignatureCountNotMatched : BlockchainSdkError(100)

    sealed class Solana(
        subCode: Int,
        customMessage: String? = null,
    ) : BlockchainSdkError(
        ERROR_CODE_SOLANA + subCode,
        customMessage ?: (ERROR_CODE_SOLANA + subCode).toString()
    ) {
        class Api(ex: Exception) : Solana(0, ex.localizedMessage ?: "Unknown api exception")
        object FailedToCreateAssociatedTokenAddress : Solana(1, "Public key conversion failed")
        object SameSourceAndDestinationAddress : Solana(2, "Same source and destination address")
        object UnsupportedTokenDestinationAddress : Solana(3)
    }

    companion object {
        const val ERROR_CODE_SOLANA = 1000
    }
}