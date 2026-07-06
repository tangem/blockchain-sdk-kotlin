package com.tangem.blockchain.blockchains.gonka

import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.extensions.preparePublicKeyByType
import com.tangem.common.card.EllipticCurve
import wallet.core.jni.AnyAddress
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKey

/**
 * Gonka is a Cosmos-SDK based chain that is not present in TrustWallet's [CoinType] registry.
 * Its addresses share the Cosmos secp256k1 key scheme but use a custom bech32 human-readable
 * prefix ("gonka"), so we reuse [CoinType.COSMOS] together with the bech32-with-HRP variants of
 * [AnyAddress] instead of the generic [com.tangem.blockchain.common.address.WalletCoreAddressService].
 */
internal class GonkaAddressService : AddressService() {

    private val coinType: CoinType = CoinType.COSMOS

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val publicKey = PublicKey(coinType.preparePublicKeyByType(walletPublicKey), coinType.publicKeyType())
        return AnyAddress(publicKey, coinType, HRP).description()
    }

    override fun validate(address: String): Boolean {
        return AnyAddress.isValidBech32(address, coinType, HRP)
    }

    private companion object {
        const val HRP = "gonka"
    }
}