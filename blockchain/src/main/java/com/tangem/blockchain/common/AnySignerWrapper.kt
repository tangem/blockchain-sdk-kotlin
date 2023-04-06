package com.tangem.blockchain.common

import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import wallet.core.java.AnySigner
import wallet.core.java.Signer
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKey
import wallet.core.jni.PublicKeyType

class AnySignerWrapper {

    fun <T : MessageLite> sign(
        walletPublicKey: Wallet.PublicKey,
        publicKeyType: PublicKeyType,
        input: MessageLite,
        coin: CoinType,
        parser: Parser<T>,
        signer: TransactionSigner? = null,
    ): Result<T> {
        return if (signer != null) {
            signWithCard(
                walletPublicKey = walletPublicKey,
                publicKeyType = publicKeyType,
                input = input,
                coin = coin,
                parser = parser,
                signer = signer,
            )
        } else {
            signWithoutCard(
                input = input,
                coin = coin,
                parser = parser,
            )
        }
    }

    private fun <T : MessageLite> signWithCard(
        walletPublicKey: Wallet.PublicKey,
        publicKeyType: PublicKeyType,
        input: MessageLite,
        coin: CoinType,
        parser: Parser<T>,
        signer: TransactionSigner,
    ): Result<T> {
        return try {
            val walletCoreSigner = WalletCoreSigner(
                sdkSigner = signer,
                publicKeyType = publicKeyType,
                publicKey = walletPublicKey,
            )
            val result = AnySigner.signExternally(input, coin, parser, walletCoreSigner)
            // We need to check this error field in WalletCoreSigner. Because thrown exception from JNI can not be
            // handled.
            val tangemError = walletCoreSigner.error

            if (tangemError != null) Result.fromTangemSdkError(tangemError) else Result.Success(result)
        } catch (e: Exception) {
            Result.Failure(BlockchainSdkError.Ton.Sign(e))
        }
    }

    private fun <T : MessageLite> signWithoutCard(
        input: MessageLite,
        coin: CoinType,
        parser: Parser<T>,
    ): Result<T> {
        return try {
            val result = AnySigner.sign(input, coin, parser)
            Result.Success(result)
        } catch (e: Exception) {
            Result.Failure(BlockchainSdkError.Ton.Sign(e))
        }
    }
}

private class WalletCoreSigner(
    private val sdkSigner: TransactionSigner,
    private val publicKey: Wallet.PublicKey,
    private val publicKeyType: PublicKeyType,
) : Signer {

    var error: TangemError? = null

    override fun getPublicKey(): PublicKey {
        return PublicKey(publicKey.blockchainKey, publicKeyType)
    }

    override fun sign(data: ByteArray?): ByteArray {
        error = null
        val signResult = runBlocking(SupervisorJob() + Dispatchers.Default) {
            sdkSigner.sign(data ?: ByteArray(0), publicKey)
        }
        return when (signResult) {
            is CompletionResult.Success -> signResult.data
            is CompletionResult.Failure -> {
                error = signResult.error
                ByteArray(0)
            }
        }
    }
}
