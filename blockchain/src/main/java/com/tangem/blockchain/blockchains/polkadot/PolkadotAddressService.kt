package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import io.emeraldpay.polkaj.ss58.SS58Type
import io.emeraldpay.polkaj.types.Address

/**
[REDACTED_AUTHOR]
 */
class PolkadotAddressService(
    val network: SS58Type.Network
) : AddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return Address(network, walletPublicKey).toString()
    }

    override fun validate(address: String): Boolean {
        return try {
            Address.from(address)
            true
        } catch (ex: Exception) {
            false
        }
    }
}