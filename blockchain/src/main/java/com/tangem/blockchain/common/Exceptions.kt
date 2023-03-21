package com.tangem.blockchain.common

import com.tangem.common.core.TangemError
import java.math.BigDecimal

abstract class BlockchainError(code: Int) : TangemError(code)

sealed class BlockchainSdkError(
    code: Int,
    override var customMessage: String = code.toString(),
    override val messageResId: Int? = null,
    override val cause: Throwable? = null,
) : BlockchainError(code) {

    data class WrappedTangemError(val tangemError: TangemError) : BlockchainSdkError(
        code = tangemError.code,
        customMessage = tangemError.customMessage,
        messageResId = tangemError.messageResId,
    )

    class NPError(forValue: String) : BlockchainSdkError(-1, "$forValue must be not NULL")
    class UnsupportedOperation(message: String = "Unsupported operation") : BlockchainSdkError(0, message)

    class SendException(val blockchain: Blockchain, message: String) : BlockchainSdkError(1, message)
    object AccountNotFound : BlockchainSdkError(2, "Account not found")
    class CreateAccountUnderfunded(val blockchain: Blockchain, val minReserve: Amount) : BlockchainSdkError(3)
    object FailedToLoadFee : BlockchainSdkError(4, "Failed to load fee")
    class CustomError(message: String) : BlockchainSdkError(5, message)

    class WrappedThrowable(throwable: Throwable) : BlockchainSdkError(
        code = 6,
        customMessage = throwable.localizedMessage ?: "Unknown exception",
        messageResId = null,
        cause = throwable,
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
        cause = throwable,
    ) {
        class Api(ex: Exception) : Solana(1, ex.localizedMessage ?: "Unknown api exception", ex)
        object FailedToCreateAssociatedTokenAddress : Solana(2, "Public key conversion failed")
        object SameSourceAndDestinationAddress : Solana(3, "Same source and destination address")
        object UnsupportedTokenDestinationAddress : Solana(4)
    }

    sealed class Polkadot(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_POLKADOT + subCode,
        customMessage = customMessage ?: (ERROR_CODE_POLKADOT + subCode).toString(),
        messageResId = null,
        cause = throwable,
    ) {
        class Api(ex: Exception) : Polkadot(1, ex.localizedMessage ?: "Unknown api exception", ex)
    }

    sealed class Kaspa(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_KASPA + subCode,
        customMessage = customMessage ?: (ERROR_CODE_KASPA + subCode).toString(),
        messageResId = null,
        cause = throwable,
    ) {
        class UtxoAmountError(val maxAmount: BigDecimal) : Kaspa(
            2, "Too many UTXO needed. Send ${maxAmount.toPlainString()} or less"
        )
    }

    companion object {
        const val ERROR_CODE_SOLANA = 1000
        const val ERROR_CODE_POLKADOT = 2000
        const val ERROR_CODE_KASPA = 3000
    }
}

fun Exception.toBlockchainSdkError(): BlockchainSdkError = if (this is BlockchainSdkError.WrappedThrowable) {
    this
} else {
    BlockchainSdkError.WrappedThrowable(this)
}