package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.trustWalletCoinType
import com.tangem.common.card.EllipticCurve
import wallet.core.jni.AnyAddress
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKey

class TrustWalletAddressService(
    blockchain: Blockchain,
) : AddressService() {

    private val coinType: CoinType = blockchain.trustWalletCoinType

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val publicKey = PublicKey(walletPublicKey, coinType.publicKeyType())
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