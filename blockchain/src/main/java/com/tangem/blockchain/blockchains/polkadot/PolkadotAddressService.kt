package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import io.emeraldpay.polkaj.ss58.SS58Type
import io.emeraldpay.polkaj.types.Address

/**
[REDACTED_AUTHOR]
 */
class PolkadotAddressService(
    blockchain: Blockchain
) : AddressService() {

    private val ss58Network = when (blockchain) {
        Blockchain.Polkadot -> SS58Type.Network.POLKADOT
        Blockchain.PolkadotTestnet -> SS58Type.Network.WESTEND
        Blockchain.Kusama -> SS58Type.Network.KUSAMA
        Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> SS58Type.Network.SUBSTRATE
        else -> throw IllegalStateException("$blockchain isn't supported")
    }

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return Address(ss58Network, walletPublicKey).toString()
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