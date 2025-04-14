package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import io.emeraldpay.polkaj.ss58.SS58Type
import io.emeraldpay.polkaj.types.Address

/**
[REDACTED_AUTHOR]
 */
internal class PolkadotAddressService(blockchain: Blockchain) : AddressService() {

    val ss58Network = when (blockchain) {
        Blockchain.Polkadot -> SS58Type.Network.POLKADOT
        Blockchain.PolkadotTestnet -> SS58Type.Network.WESTEND
        Blockchain.Kusama -> SS58Type.Network.KUSAMA
        Blockchain.AlephZero,
        Blockchain.AlephZeroTestnet,
        Blockchain.EnergyWebX,
        Blockchain.EnergyWebXTestnet,
        -> SS58Type.Network.SUBSTRATE
        Blockchain.Joystream -> SS58Type.Network.JOYSTREAM
        Blockchain.Bittensor -> SS58Type.Network.BITTENSOR
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