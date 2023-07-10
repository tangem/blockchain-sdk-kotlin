package com.tangem.blockchain.common

import com.tangem.blockchain.common.address.AddressProvider
import com.tangem.blockchain.common.address.AddressType

class WalletFactory(private val addressProvider: AddressProvider) {

    fun makeWallet(blockchain: Blockchain, publicKeys: Map<AddressType, Wallet.PublicKey>): Wallet {
        require(publicKeys.containsKey(AddressType.Default)) { "PublicKeys have to contain default publicKey" }

        val addresses = publicKeys.mapValues { (addressType, publicKey) ->
            addressProvider.makeAddress(publicKey, addressType)
        }

        return Wallet(blockchain, addresses, emptySet())
    }
}