package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.card.EllipticCurve
import io.emeraldpay.polkaj.ss58.SS58Type
import io.emeraldpay.polkaj.types.Address

/**
 * Created by Anton Zhilenkov on 10/06/2022.
 */
class PolkadotAddressService(
    blockchain: Blockchain,
) : AddressService {

    private val ss58Network = when (blockchain) {
        Blockchain.Polkadot -> SS58Type.Network.POLKADOT
        Blockchain.PolkadotTestnet -> SS58Type.Network.WESTEND
        Blockchain.Kusama -> SS58Type.Network.KUSAMA
        Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> SS58Type.Network.SUBSTRATE
        else -> throw IllegalStateException("$blockchain isn't supported")
    }

    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve,
    ): PlainAddress {
        return PlainAddress(
            value = Address(ss58Network, publicKey.blockchainKey).toString(),
            type = AddressType.Default,
            publicKey = publicKey
        )
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