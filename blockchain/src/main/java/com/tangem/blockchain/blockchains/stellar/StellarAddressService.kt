package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.card.EllipticCurve
import org.stellar.sdk.KeyPair

class StellarAddressService : AddressService {

    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve,
    ): PlainAddress {
        val kp = KeyPair.fromPublicKey(publicKey.blockchainKey)
        return PlainAddress(
            value = kp.accountId,
            type = addressType,
            publicKey = publicKey
        )
    }

    override fun validate(address: String): Boolean {
        return try {
            KeyPair.fromAccountId(address) != null
        } catch (exception: Exception) {
            false
        }
    }
}