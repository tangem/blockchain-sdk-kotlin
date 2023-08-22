package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.bitcoin.BitcoinScriptAddressesProvider
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.card.EllipticCurve
import java.lang.IllegalStateException

class WalletFactory(
    private val blockchain: Blockchain,
    private val addressService: AddressService
) {

    @Throws(Exception::class)
    fun makeWallet(publicKey: Wallet.PublicKey, curve: EllipticCurve): Wallet {
        // Temporary for get count on addresses
        val addressTypes: Array<AddressType> = blockchain.derivationPaths(DerivationStyle.V2).keys.toTypedArray()

        val addresses: MutableMap<AddressType, PlainAddress> = mutableMapOf()

        for (addressType in addressTypes) {
            addresses[addressType] = addressService.makeAddress(publicKey, addressType, curve)
        }

        return Wallet(
            blockchain = blockchain,
            walletAddresses = addresses
        )
    }

    /// With multisig script public key
    fun makeWallet(publicKey: Wallet.PublicKey, pairPublicKey: ByteArray): Wallet {
        val addressProvider = addressService as? BitcoinScriptAddressesProvider
            ?: throw IllegalStateException("$addressService must be BitcoinScriptAddressesProvider")

        val addresses = addressProvider.makeAddresses(publicKey, pairPublicKey)

        val addressMap = addresses.associateBy { it.type }

        return Wallet(
            blockchain = blockchain,
            walletAddresses = addressMap
        )
    }

}