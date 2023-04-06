package com.tangem.blockchain.common

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import wallet.core.java.Signer
import wallet.core.jni.PublicKey
import wallet.core.jni.PublicKeyType

class WalletCoreSigner(
    private val sdkSigner: TransactionSigner,
    private val publicKey: Wallet.PublicKey,
    private val publicKeyType: PublicKeyType,
) : Signer {

    internal var error: TangemError? = null
        private set

    override fun getPublicKey(): PublicKey {
        return PublicKey(publicKey.blockchainKey, publicKeyType)
    }

    override fun sign(data: ByteArray?): ByteArray {
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