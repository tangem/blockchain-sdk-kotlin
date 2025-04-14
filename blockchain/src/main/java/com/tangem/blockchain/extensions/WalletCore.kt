package com.tangem.blockchain.extensions

import com.tangem.common.extensions.toCompressedPublicKey
import wallet.core.jni.PublicKey
import wallet.core.jni.PublicKeyType

/**
 * Creates WalletCore public key and compresses it if needed
 */
fun createWalletCorePublicKey(data: ByteArray, keyType: PublicKeyType): PublicKey {
    return when (keyType) {
        PublicKeyType.SECP256K1 -> PublicKey(data.toCompressedPublicKey(), keyType)
        else -> PublicKey(data, keyType)
    }
}