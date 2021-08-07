package com.tangem.blockchain.common

import com.tangem.common.core.TangemError


class CreateAccountUnderfunded(val minReserve: Amount) : Exception()
class SendException(message: String) : Exception(message)

sealed class BlockchainSdkError : TangemError, Exception() {
    object SignatureCountNotMatched : BlockchainSdkError() {
        override val code: Int = 0
        override var customMessage: String = code.toString()
        override val messageResId: Int? = null
    }
}