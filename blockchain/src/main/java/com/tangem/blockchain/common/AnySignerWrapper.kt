package com.tangem.blockchain.common

import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import com.tangem.blockchain.extensions.Result
import wallet.core.java.AnySigner
import wallet.core.jni.CoinType

class AnySignerWrapper {

    fun <T : MessageLite> sign(
        input: MessageLite,
        coin: CoinType,
        parser: Parser<T>,
        signer: WalletCoreSigner? = null,
    ): Result<T> {
        val result = if (signer != null) {
            AnySigner.signExternally(input, coin, parser, signer)
        } else {
            AnySigner.sign(input, coin, parser)
        }
        val tangemError = signer?.error

        return if (tangemError != null) Result.fromTangemSdkError(tangemError) else Result.Success(result)
    }
}