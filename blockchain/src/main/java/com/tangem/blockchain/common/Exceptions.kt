package com.tangem.blockchain.common

import com.tangem.common.core.TangemError


sealed class BlockchainSdkError(
    override val code: Int = 0,
    override var customMessage: String = code.toString(),
    override val messageResId: Int? = null
) : TangemError, Exception() {

    class UnsupportedOperation(message: String = "Unsupported operation") : BlockchainSdkError(0, message)
    class SendException(val blockchain: Blockchain, message: String) : BlockchainSdkError(1, message)
    class CreateAccountUnderfunded(val minReserve: Amount) : BlockchainSdkError(3)

    object FailedToLoadFee : BlockchainSdkError(1, "Failed to load fee")
    object AccountNotFound : BlockchainSdkError(2, "Account not found")

    object SignatureCountNotMatched : BlockchainSdkError(100)
    object WrongDerivationPath : BlockchainSdkError(101)

    class AddingTokenAmountError(token: Token)
        : BlockchainSdkError(102, "Can't add the token amount for $token")
}