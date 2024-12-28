package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.preparePublicKeyByType
import com.tangem.blockchain.extensions.trustWalletCoinType
import com.tangem.common.card.EllipticCurve
import wallet.core.jni.AnyAddress
import wallet.core.jni.Cardano
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKey

internal open class WalletCoreAddressService(
    private val blockchain: Blockchain,
) : AddressService() {

    private val coinType: CoinType = blockchain.trustWalletCoinType

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val publicKey = PublicKey(coinType.preparePublicKeyByType(walletPublicKey), coinType.publicKeyType())
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

    override fun makeAddresses(walletPublicKey: ByteArray, curve: EllipticCurve?): Set<Address> {
        return if (blockchain == Blockchain.Cardano) {
            setOf(
                Address(makeAddress(walletPublicKey), AddressType.Default),
                Address(
                    value = Cardano.getByronAddress(PublicKey(walletPublicKey, coinType.publicKeyType())),
                    type = AddressType.Legacy,
                ),
            )
        } else {
            super.makeAddresses(walletPublicKey, curve)
        }
    }
}