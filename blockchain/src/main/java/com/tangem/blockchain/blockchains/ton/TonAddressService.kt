package com.tangem.blockchain.blockchains.ton

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.extensions.compressPublicKeyIfNeeded
import com.tangem.blockchain.extensions.trustWalletCoinType
import com.tangem.common.card.EllipticCurve
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKey
import wallet.core.jni.TheOpenNetworkAddress

internal class TonAddressService(blockchain: Blockchain) : AddressService() {

    private val coinType: CoinType = blockchain.trustWalletCoinType

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val tonAddress = makeTheOpenNetworkAddress(walletPublicKey)
        return tonAddress.stringRepresentation(/*userFriendly*/ true, /*bounceable*/ false, /*testOnly*/ false)
    }

    override fun validate(address: String): Boolean {
        return TheOpenNetworkAddress.isValidString(address)
    }

    fun makeTheOpenNetworkAddress(walletPublicKey: ByteArray): TheOpenNetworkAddress {
        val publicKey = PublicKey(coinType.compressPublicKeyIfNeeded(walletPublicKey), coinType.publicKeyType())
        return TheOpenNetworkAddress(publicKey)
    }
}