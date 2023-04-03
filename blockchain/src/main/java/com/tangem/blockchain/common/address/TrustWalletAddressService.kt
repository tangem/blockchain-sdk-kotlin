package com.tangem.blockchain.common.address

import com.tangem.common.card.EllipticCurve
import wallet.core.jni.AnyAddress
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKey
import wallet.core.jni.PublicKeyType

class TrustWalletAddressService(
    private val coinType: CoinType,
    private val publicKeyType: PublicKeyType,
) : AddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val publicKey = PublicKey(walletPublicKey, publicKeyType)
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
}