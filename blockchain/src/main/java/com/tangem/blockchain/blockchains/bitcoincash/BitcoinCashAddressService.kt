package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.BitcoinCashAddressType
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.CashAddr
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.*
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey
import java.lang.IllegalStateException

class BitcoinCashAddressService(blockchain: Blockchain) : AddressService {

    private val cashAddr = when (blockchain) {
        Blockchain.BitcoinCash -> CashAddr(false)
        Blockchain.BitcoinCashTestnet -> CashAddr(true)
        else -> throw IllegalStateException(
            "${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}"
        )
    }

    private val legacyService = BitcoinAddressService(Blockchain.Bitcoin).legacy

    override fun makeAddress(publicKey: Wallet.PublicKey, addressType: AddressType, curve: EllipticCurve): PlainAddress {
        return when(addressType) {
            AddressType.Default -> makeCashAddrAddress(publicKey)
            AddressType.Legacy -> makeLegacyAddress(publicKey)
        }
    }

    override fun validate(address: String) =
        validateCashAddrAddress(address) || legacyService.validate(address)

    private fun makeCashAddrAddress(publicKey: Wallet.PublicKey): PlainAddress {
        val publicKeyHash = publicKey.blockchainKey.toCompressedPublicKey().calculateSha256().calculateRipemd160()
        val address = cashAddr.toCashAddress(BitcoinCashAddressType.P2PKH, publicKeyHash)
        return PlainAddress(address, AddressType.Default, publicKey)
    }

    private fun makeLegacyAddress(publicKey: Wallet.PublicKey) : PlainAddress {
        val address = legacyService.makeAddressOldStyle(publicKey.blockchainKey.toCompressedPublicKey()).value
        return PlainAddress(
            value = address,
            type = AddressType.Legacy,
            publicKey = publicKey
        )
    }

    fun validateCashAddrAddress(address: String) = cashAddr.isValidCashAddress(address)

    fun getPublicKeyHash(address: String): ByteArray {
        return cashAddr.decodeCashAddress(address).hash
    }
}