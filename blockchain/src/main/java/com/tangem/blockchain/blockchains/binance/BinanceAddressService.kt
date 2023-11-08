package com.tangem.blockchain.blockchains.binance

import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.PlainAddress
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.Bech32

class BinanceAddressService(private val testNet: Boolean = false) : AddressService {

    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve,
    ): PlainAddress {
        val publicKeyHash = publicKey.blockchainKey.toCompressedPublicKey().calculateSha256().calculateRipemd160()

        val humanReadablePart = if (testNet) "tbnb" else "bnb"

        val address = Bech32.encode(
            humanReadablePart,
            Crypto.convertBits(publicKeyHash, 0, publicKeyHash.size, 8, 5, false)
        )

        return PlainAddress(
            value = address,
            type = addressType,
            publicKey = publicKey
        )
    }

    override fun validate(address: String): Boolean {
        return try {
            Crypto.decodeAddress(address)
            if (testNet) {
                address.startsWith("tbnb1")
            } else {
                address.startsWith("bnb1")
            }
        } catch (exception: Exception) {
            false
        }
    }
}

enum class BinanceChain(val value: String) {
    Nile("Binance-Chain-Nile"),
    Tigris("Binance-Chain-Tigris");

    companion object {
        fun getChain(testNet: Boolean): BinanceChain = if (testNet) Nile else Tigris
    }
}
