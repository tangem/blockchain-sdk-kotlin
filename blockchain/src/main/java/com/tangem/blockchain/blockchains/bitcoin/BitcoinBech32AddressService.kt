package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressProvider
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.AddressValidator
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.script.Script

class BitcoinBech32AddressService(
    val blockchain: Blockchain,
    val networkParameters: NetworkParameters,
) : BitcoinScriptAddressProvider, AddressValidator, AddressProvider {

    override fun makeScriptAddress(script: Script): String {
        val scriptHash = script.program.calculateSha256()
        val address = SegwitAddress.fromHash(networkParameters, scriptHash)

        return address.toBech32()
    }

    override fun validate(address: String): Boolean {
        return validateSegwitAddress(address)
    }

    override fun makeAddress(publicKey: Wallet.PublicKey, addressType: AddressType): PlainAddress {
        val compressedPublicKey = ECKey.fromPublicOnly(publicKey.blockchainKey.toCompressedPublicKey())
        val address = SegwitAddress.fromKey(networkParameters, compressedPublicKey).toBech32()
        return PlainAddress(address, addressType, publicKey)
    }

    private fun validateSegwitAddress(address: String): Boolean {
        return try {
            if (blockchain == Blockchain.Ducatus) return false
            SegwitAddress.fromBech32(networkParameters, address)
            true
        } catch (e: Exception) {
            false
        }
    }

}