package com.tangem.blockchain.common

import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.crypto.Secp256k1
import com.tangem.crypto.generateKeyPair
import com.tangem.crypto.sign

class DummySigner : TransactionSigner {

    val keyPair: KeyPair = Secp256k1.generateKeyPair()

    val publicKey = Wallet.PublicKey(
        keyPair.publicKey.toCompressedPublicKey(), null, null
    )


    override suspend fun sign(
        hashes: List<ByteArray>,
        cardId: String,
        publicKey: Wallet.PublicKey
    ): CompletionResult<List<ByteArray>> {
        return CompletionResult.Success(
            hashes.map { (sign(it, cardId, publicKey) as CompletionResult.Success).data }
        )
    }

    override suspend fun sign(
        hash: ByteArray,
        cardId: String,
        publicKey: Wallet.PublicKey
    ): CompletionResult<ByteArray> {
        return CompletionResult.Success(hash.sign(keyPair.privateKey))
    }
}