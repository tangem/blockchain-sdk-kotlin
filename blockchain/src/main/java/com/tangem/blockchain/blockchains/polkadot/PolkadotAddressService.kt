package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import io.emeraldpay.polkaj.types.Address

/**
[REDACTED_AUTHOR]
 */
class PolkadotAddressService(
    private val blockchain: Blockchain
) : AddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return Address(PolkadotNetworkService.network(blockchain), walletPublicKey).toString()
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