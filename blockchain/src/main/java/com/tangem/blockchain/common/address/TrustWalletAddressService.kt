package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.trustWalletCoinType
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toCompressedPublicKey
import wallet.core.jni.AnyAddress
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKey
import wallet.core.jni.PublicKeyType

class TrustWalletAddressService(
    blockchain: Blockchain
) : AddressService {

    private val coinType: CoinType = blockchain.trustWalletCoinType

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val publicKey = PublicKey(compressIfNeeded(walletPublicKey), coinType.publicKeyType())
        return AnyAddress(publicKey, coinType).description()
    }

    override fun validate(address: String): Boolean {
        val anyAddress: AnyAddress? = try {
            AnyAddress(address, coinType)
        } catch (_: Exception) {
            null
        }
        return anyAddress != null
    }

    private fun compressIfNeeded(data: ByteArray): ByteArray {
        return if (coinType.publicKeyType() == PublicKeyType.SECP256K1) data.toCompressedPublicKey() else data
    }
}
