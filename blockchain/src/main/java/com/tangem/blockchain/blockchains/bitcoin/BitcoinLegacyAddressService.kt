package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressProvider
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.AddressValidator
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.script.Script

class BitcoinLegacyAddressService(
    val blockchain: Blockchain,
    val networkParameters: NetworkParameters,
) : BitcoinScriptAddressProvider, AddressValidator, AddressProvider {

    override fun makeScriptAddress(script: Script): String {
        val scriptHash = script.program.calculateSha256().calculateRipemd160()
        val address = LegacyAddress.fromScriptHash(networkParameters, scriptHash)
        return address.toBase58()
    }

    override fun validate(address: String): Boolean {
        return validateLegacyAddress(address)
    }

    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve,
    ): PlainAddress {
        val ecPublicKey = ECKey.fromPublicOnly(publicKey.blockchainKey)
        val address = LegacyAddress.fromKey(networkParameters, ecPublicKey).toBase58()
        return PlainAddress(address, addressType, publicKey)
    }

    private fun validateLegacyAddress(address: String): Boolean {
        return try {
            LegacyAddress.fromBase58(networkParameters, address)
            true
        } catch (e: Exception) {
            false
        }
    }
}