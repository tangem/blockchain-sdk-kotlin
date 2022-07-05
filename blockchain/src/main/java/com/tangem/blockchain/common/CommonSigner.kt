package com.tangem.blockchain.common

import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

open class CommonSigner(
    private val tangemSdk: TangemSdk,
    var cardId: String? = null,
    var initialMessage: Message? = null
) : TransactionSigner {

    override suspend fun sign(hashes: List<ByteArray>, publicKey: Wallet.PublicKey): CompletionResult<List<ByteArray>> =
        suspendCancellableCoroutine { continuation ->
            tangemSdk.sign(
                hashes = hashes.toTypedArray(),
                walletPublicKey = publicKey.seedKey,
                cardId = cardId,
                initialMessage = initialMessage,
            ) { result ->
                when (result) {
                    is CompletionResult.Success -> continuation.resume(CompletionResult.Success(result.data.signatures))
                    is CompletionResult.Failure -> continuation.resume(CompletionResult.Failure(result.error))
                }
            }
        }

    override suspend fun sign(hash: ByteArray, publicKey: Wallet.PublicKey): CompletionResult<ByteArray> =
        suspendCancellableCoroutine { continuation ->
            tangemSdk.sign(
                hash = hash,
                walletPublicKey = publicKey.seedKey,
                cardId = cardId,
                initialMessage = initialMessage,
            ) { result ->
                when (result) {
                    is CompletionResult.Success -> continuation.resume(CompletionResult.Success(result.data.signature))
                    is CompletionResult.Failure -> continuation.resume(CompletionResult.Failure(result.error))
                }
            }
        }
}