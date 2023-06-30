package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import org.stellar.sdk.KeyPair

class StellarAddressService: AddressService {
    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val kp = KeyPair.fromPublicKey(walletPublicKey)
        return kp.accountId
    }

    override fun validate(address: String): Boolean {
        return try {
            KeyPair.fromAccountId(address) != null
        } catch (exception: Exception) {
            false
        }
    }
}