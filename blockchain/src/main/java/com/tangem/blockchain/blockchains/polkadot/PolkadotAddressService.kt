package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import io.emeraldpay.polkaj.ss58.SS58Type
import io.emeraldpay.polkaj.types.Address

/**
 * Created by Anton Zhilenkov on 10/06/2022.
 */
class PolkadotAddressService(
    blockchain: Blockchain,
) : AddressService() {

    private val ss58Network = when (blockchain) {
        Blockchain.Polkadot -> SS58Type.Network.POLKADOT
        Blockchain.PolkadotTestnet -> SS58Type.Network.WESTEND
        Blockchain.Kusama -> SS58Type.Network.KUSAMA
        Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> SS58Type.Network.SUBSTRATE
        else -> error("$blockchain isn't supported")
    }

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return Address(ss58Network, walletPublicKey).toString()
    }

    override fun validate(address: String): Boolean {
        return try {
            val polkadotAddress = Address.from(address)
            polkadotAddress.network == ss58Network
        } catch (ex: Exception) {
            false
        }
    }
}
