package com.tangem.blockchain.common

import com.tangem.common.core.TangemError

interface BlockchainError : TangemError

sealed class BlockchainSdkError(
    override val code: Int,
    override var customMessage: String = code.toString(),
    override val messageResId: Int? = null,
    val throwable: Throwable? = null
) : Exception(code.toString(), throwable), BlockchainError {

    data class WrappedTangemError(val tangemError: TangemError) : BlockchainSdkError(
        code = tangemError.code,
        customMessage = tangemError.customMessage,
        messageResId = tangemError.messageResId,
    )

    class NPError(forValue: String) : BlockchainSdkError(-1, "$forValue must be not NULL")
    class UnsupportedOperation(message: String = "Unsupported operation") : BlockchainSdkError(0, message)

    class SendException(val blockchain: Blockchain, message: String) : BlockchainSdkError(1, message)
    object AccountNotFound : BlockchainSdkError(2, "Account not found")
    class CreateAccountUnderfunded(val minReserve: Amount) : BlockchainSdkError(3)
    object FailedToLoadFee : BlockchainSdkError(4, "Failed to load fee")
    class CustomError(message: String) : BlockchainSdkError(5, message)

    class WrappedThrowable(throwable: Throwable) : BlockchainSdkError(
        code = 6,
        customMessage = throwable.localizedMessage ?: "Unknown exception",
        messageResId = null,
        throwable = throwable,
    )

    object SignatureCountNotMatched : BlockchainSdkError(100)

    sealed class Solana(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null
    ) : BlockchainSdkError(
        code = ERROR_CODE_SOLANA + subCode,
        customMessage = customMessage ?: (ERROR_CODE_SOLANA + subCode).toString(),
        messageResId = null,
        throwable = throwable,
    ) {
        class Api(ex: Exception) : Solana(0, ex.localizedMessage ?: "Unknown api exception", ex)
        object FailedToCreateAssociatedTokenAddress : Solana(1, "Public key conversion failed")
        object SameSourceAndDestinationAddress : Solana(2, "Same source and destination address")
        object UnsupportedTokenDestinationAddress : Solana(3)
    }

    sealed class Polkadot(
        subCode: Int,
        customMessage: String? = null,
    ) : BlockchainSdkError(
        ERROR_CODE_POLKADOT + subCode,
        customMessage ?: (ERROR_CODE_POLKADOT + subCode).toString()
    ) {
        object ExistentialDepositError : Polkadot(1, "%s network has a concept of Existential Deposit. If your account drops below %s it will be deactivated and any remaining funds will be destroyed. To avoid that you can reduce the amount by %s.")
        object ExistentialDepositReduce : Polkadot(2, "Reduce by %s")
        object ExistentialDepositIgnore : Polkadot(3, "No, send all")
    }

    companion object {
        const val ERROR_CODE_SOLANA = 1000
        const val ERROR_CODE_POLKADOT = 2000
    }
}

fun Exception.toBlockchainSdkError(): BlockchainSdkError = if (this is BlockchainSdkError.WrappedThrowable) {
    this
} else {
    BlockchainSdkError.WrappedThrowable(this)
}