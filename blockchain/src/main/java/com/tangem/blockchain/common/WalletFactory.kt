package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.bitcoin.BitcoinScriptAddressesProvider
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.card.EllipticCurve
import java.lang.IllegalStateException

class WalletFactory(
    private val blockchain: Blockchain,
    private val ellipticCurve: EllipticCurve,
) {

    private val addressProvider = AddressServiceFactory(blockchain, ellipticCurve).makeAddressService()

    @Throws(Exception::class)
    fun makeWallet(publicKey: Wallet.PublicKey): Wallet {
        // Temporary for get count on addresses
        val addressTypes: Array<AddressType> = blockchain.derivationPaths(DerivationStyle.V2).keys.toTypedArray()

        val addresses: MutableMap<AddressType, PlainAddress> = mutableMapOf()

        for (addressType in addressTypes) {
            addresses[addressType] = addressProvider.makeAddress(publicKey, addressType)
        }

        return Wallet(
            blockchain = blockchain,
            walletAddresses = addresses
        )
    }

    /// With multisig script public key
    fun makeWallet(publicKey: Wallet.PublicKey, pairPublicKey: ByteArray): Wallet {
        val addressProvider = addressProvider as? BitcoinScriptAddressesProvider
            ?: throw IllegalStateException("$addressProvider must be BitcoinScriptAddressesProvider")

        val addresses = addressProvider.makeAddresses(publicKey, pairPublicKey)

        val addressMap = addresses.associateBy { it.type }

        return Wallet(
            blockchain = blockchain,
            walletAddresses = addressMap
        )
    }

    // with different public keys
    fun makeWallet(publicKeys: Map<AddressType, Wallet.PublicKey>): Wallet {
        require(publicKeys.containsKey(AddressType.Default)) { "PublicKeys have to contain default publicKey" }

        val addresses = publicKeys.mapValues { (addressType, publicKey) ->
            addressProvider.makeAddress(publicKey, addressType)
        }

        return Wallet(
            blockchain = blockchain,
            walletAddresses = addresses
        )
    }
}