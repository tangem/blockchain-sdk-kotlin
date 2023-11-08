package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.walletCoreWalletType
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toCompressedPublicKey
import wallet.core.jni.AnyAddress
import wallet.core.jni.Cardano
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKey
import wallet.core.jni.PublicKeyType

class WalletCoreAddressService(blockchain: Blockchain) : AddressService {

    private val coinType: CoinType = blockchain.walletCoreWalletType

    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve,
    ): PlainAddress {
        val pk = PublicKey(compressIfNeeded(publicKey.blockchainKey), coinType.publicKeyType())
        val address = AnyAddress(pk, coinType).description()

        return PlainAddress(
            value = address,
            type = AddressType.Default,
            publicKey = publicKey
        )
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
