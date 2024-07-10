@file:Suppress("MagicNumber")

package com.tangem.blockchain.common

import com.tangem.common.core.TangemError
import java.math.BigDecimal

@Suppress("UnnecessaryAbstractClass")
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

    data object FailedToSendException : BlockchainSdkError(1, "Failed to send transaction")
    data class AccountNotFound(val amountToCreateAccount: BigDecimal? = null) : BlockchainSdkError(
        2,
        "Account not found",
    )

    class CreateAccountUnderfunded(val blockchain: Blockchain, val minReserve: Amount) : BlockchainSdkError(3)
    data object FailedToLoadFee : BlockchainSdkError(4, "Failed to load fee")
    class CustomError(message: String) : BlockchainSdkError(5, message)

    class WrappedThrowable(throwable: Throwable) : BlockchainSdkError(
        code = 6,
        customMessage = throwable.localizedMessage ?: "Unknown exception",
        messageResId = null,
        cause = throwable,
    )

    data object FailedToBuildTx : BlockchainSdkError(7, "Failed to build transaction")

    data object FailedToCreateAccount : BlockchainSdkError(8, "Failed to create account")

    data object SignatureCountNotMatched : BlockchainSdkError(100)

    sealed class Solana(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_SOLANA + subCode,
        customMessage = customMessage ?: (ERROR_CODE_SOLANA + subCode).toString(),
        messageResId = null,
        cause = throwable,
    ) {
        class Api(ex: Exception) : Solana(1, ex.localizedMessage ?: "Unknown api exception", ex)
        data object FailedToCreateAssociatedAccount : Solana(2, "Public key conversion failed")
        data object SameSourceAndDestinationAddress : Solana(3, "Same source and destination address")
        data object UnsupportedTokenDestinationAddress : Solana(4)
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
        class ApiWithCode(code: Int, message: String) : Polkadot(code, message)
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
        class UtxoAmountError(val maxOutputs: Int, val maxAmount: BigDecimal) : Kaspa(
            2,
            "Due to Kaspa limitations only $maxOutputs UTXOs can fit in a single transaction. This means you can only" +
                " send ${maxAmount.toPlainString()}. You need to reduce the amount",
        )
    }

    sealed class Ton(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_TON + subCode,
        customMessage = customMessage ?: "${ERROR_CODE_TON + subCode}",
        messageResId = null,
        cause = throwable,
    ) {
        class Api(code: Int, message: String) : Ton(subCode = code, customMessage = message)
    }

    sealed class Cosmos(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_COSMOS + subCode,
        customMessage = customMessage ?: "${ERROR_CODE_COSMOS + subCode}",
        messageResId = null,
        cause = throwable,
    ) {
        class Api(code: Int, message: String) : Cosmos(subCode = code, customMessage = message)
    }

    sealed class NearException(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_NEAR + subCode,
        customMessage = customMessage ?: "${ERROR_CODE_NEAR + subCode}",
        messageResId = null,
        cause = throwable,
    ) {
        class Api(val name: String, code: Int, message: String) : NearException(
            subCode = code,
            customMessage = message,
        )
    }

    class WalletCoreException(
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_WALLET_CORE,
        customMessage = "Exception while signing transaction. $customMessage",
        messageResId = null,
        cause = throwable,
    )

    sealed class Chia(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_CHIA + subCode,
        customMessage = customMessage ?: (ERROR_CODE_CHIA + subCode).toString(),
        messageResId = null,
        cause = throwable,
    ) {
        class UtxoAmountError(val maxOutputs: Int, val maxAmount: BigDecimal) : Chia(
            1,
            "Due to Chia limitations only $maxOutputs UTXOs can fit in a single transaction. This means you can only" +
                " send ${maxAmount.toPlainString()}.",
        )
    }

    sealed class Ethereum(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_ETHEREUM + subCode,
        customMessage = customMessage ?: "${ERROR_CODE_ETHEREUM + subCode}",
        messageResId = null,
        cause = throwable,
    ) {
        class Api(code: Int, message: String) : Ethereum(subCode = code, customMessage = message)
    }

    sealed class Algorand(
        subCode: Int,
        customMessage: String? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_ALGORAND + subCode,
        customMessage = customMessage ?: "${ERROR_CODE_ALGORAND + subCode}",
    ) {
        class Send(message: String) : Algorand(subCode = 0, customMessage = message)
    }

    sealed class ElectrumBlockchain(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_ELECTRUM,
        customMessage = customMessage?.let { "$ERROR_CODE_ELECTRUM: $subCode: $customMessage" }
            ?: "$ERROR_CODE_ELECTRUM: $subCode",
        messageResId = null,
        cause = throwable,
    ) {
        class Api(code: Int, message: String) : ElectrumBlockchain(subCode = code, customMessage = message)
    }

    sealed class Koinos(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_KOINOS,
        customMessage = customMessage?.let { "$ERROR_CODE_KOINOS: $subCode: $customMessage" }
            ?: "$ERROR_CODE_KOINOS: $subCode",
        messageResId = null,
        cause = throwable,
    ) {
        class Api(code: Int, message: String) : Koinos(subCode = code, customMessage = message)

        data class InsufficientMana(
            val manaBalance: BigDecimal? = null,
            val maxMana: BigDecimal? = null,
        ) : Koinos(subCode = -32603, customMessage = "Insufficient Mana")

        data class ManaFeeExceedsBalance(
            val availableKoinForTransfer: BigDecimal,
        ) : Koinos(
            subCode = 1,
            customMessage = "You can transfer only $availableKoinForTransfer KOIN" +
                " due to the Mana limit imposed by the Koinos network.",
        )

        data object InsufficientBalance : Koinos(
            subCode = 2,
            customMessage = "Insufficient Balance. Your balance should be higher than the fee value to make a transfer",
        )

        class ProtobufDecodeError(protoType: String) : Koinos(
            subCode = 999999,
            customMessage = "Failed to decode $protoType",
        )
    }

    sealed class Cardano(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_CARDANO,
        customMessage = customMessage?.let { "$ERROR_CODE_CARDANO: $subCode: $customMessage" }
            ?: "$ERROR_CODE_CARDANO: $subCode",
        messageResId = null,
        cause = throwable,
    ) {

        data object InsufficientRemainingBalance : Cardano(
            subCode = 0,
            customMessage = "Insufficient ADA balance. Make sure the balance after this transaction is at least 1 ADA.",
        )

        data object InsufficientRemainingBalanceToWithdrawTokens : Cardano(
            subCode = 1,
            customMessage = "Insufficient ADA balance. Make sure that the balance after this transaction is " +
                "sufficient to cover the withdrawal of all tokens.",
        )

        data object InsufficientSendingAdaAmount : Cardano(
            subCode = 2,
            customMessage = "Insufficient sending ADA amount. Make sure the sending amount is at least 1 ADA.",
        )

        data object InsufficientMinAdaBalanceToSendToken : Cardano(
            subCode = 3,
            customMessage = "Insufficient min-ada-value amount. In addition to network fees, the Cardano network " +
                "charges min-ada-value. Make sure the balance is sufficient to withdraw the token.",
        )
    }

    sealed class Aptos(
        subCode: Int,
        customMessage: String? = null,
        throwable: Throwable? = null,
    ) : BlockchainSdkError(
        code = ERROR_CODE_APTOS,
        customMessage = customMessage?.let { "$ERROR_CODE_APTOS: $subCode: $customMessage" }
            ?: "$ERROR_CODE_APTOS: $subCode",
        messageResId = null,
        cause = throwable,
    ) {
        class Api(message: String) : Aptos(subCode = 0, customMessage = message)
    }

    companion object {
        const val ERROR_CODE_SOLANA = 1000
        const val ERROR_CODE_POLKADOT = 2000
        const val ERROR_CODE_KASPA = 3000
        const val ERROR_CODE_TON = 4000
        const val ERROR_CODE_COSMOS = 5000
        const val ERROR_CODE_WALLET_CORE = 6000
        const val ERROR_CODE_CHIA = 7000
        const val ERROR_CODE_NEAR = 8000
        const val ERROR_CODE_ETHEREUM = 9000
        const val ERROR_CODE_ALGORAND = 10000
        const val ERROR_CODE_ELECTRUM = 11000
        const val ERROR_CODE_KOINOS = 12000
        const val ERROR_CODE_CARDANO = 13000
        const val ERROR_CODE_APTOS = 14000
    }
}

fun Exception.toBlockchainSdkError(): BlockchainSdkError = if (this is BlockchainSdkError.WrappedThrowable) {
    this
} else {
    BlockchainSdkError.WrappedThrowable(this)
}