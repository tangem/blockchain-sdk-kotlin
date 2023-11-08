package com.tangem.blockchain.blockchains.cardano

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.*
import com.tangem.common.card.EllipticCurve

class CardanoAddressServiceFacade : AddressService {

    private val legacyService = CardanoAddressService(Blockchain.Cardano)
    private val trustWalletService = WalletCoreAddressService(Blockchain.Cardano)

    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve
    ): PlainAddress {
        return if (publicKey.blockchainKey.isExtendedPublicKey()) {
            trustWalletService.makeAddress(publicKey, addressType, curve)
        } else {
            legacyService.makeAddress(publicKey, addressType, curve)
        }
    }

    override fun validate(address: String): Boolean {
        return trustWalletService.validate(address) || legacyService.validate(address)
    }

    private fun ByteArray.isExtendedPublicKey() = this.size == 128
}
