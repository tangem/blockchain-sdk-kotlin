package com.tangem.blockchain.common

import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.crypto.Secp256k1
import com.tangem.crypto.generateKeyPair
import com.tangem.crypto.sign

class DummySigner : TransactionSigner {

    private val keyPair: KeyPair = Secp256k1.generateKeyPair()

    val publicKey = Wallet.PublicKey(
        seedKey = keyPair.publicKey.toCompressedPublicKey(),
        derivationType = null
    )

    override suspend fun sign(
        hashes: List<ByteArray>,
        publicKey: Wallet.PublicKey
    ): CompletionResult<List<ByteArray>> {
        return CompletionResult.Success(
            hashes.map { (sign(it, publicKey) as CompletionResult.Success).data }
        )
    }

    override suspend fun sign(
        hash: ByteArray,
        publicKey: Wallet.PublicKey
    ): CompletionResult<ByteArray> {
        return CompletionResult.Success(hash.sign(keyPair.privateKey))
    }

}