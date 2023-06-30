package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.BitcoinCashAddressType
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.CashAddr
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.MultipleAddressProvider
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey

class BitcoinCashAddressService(blockchain: Blockchain) : MultipleAddressProvider {

    private val cashAddr = when (blockchain) {
        Blockchain.BitcoinCash -> CashAddr(false)
        Blockchain.BitcoinCashTestnet -> CashAddr(true)
        else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    private val bitcoinAddressService = BitcoinAddressService(Blockchain.Bitcoin)

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?) =
        makeCashAddrAddress(walletPublicKey).value

    override fun makeAddresses(walletPublicKey: ByteArray, curve: EllipticCurve?) =
        setOf(
            bitcoinAddressService.makeLegacyAddress(walletPublicKey.toCompressedPublicKey()),
            makeCashAddrAddress(walletPublicKey)
        )

    override fun validate(address: String) =
        validateCashAddrAddress(address) || bitcoinAddressService.validateLegacyAddress(address)

    private fun makeCashAddrAddress(walletPublicKey: ByteArray): Address {
        val publicKeyHash = walletPublicKey.toCompressedPublicKey().calculateSha256().calculateRipemd160()
        val address = cashAddr.toCashAddress(BitcoinCashAddressType.P2PKH, publicKeyHash)
        return Address(address, AddressType.Default)
    }

    fun validateCashAddrAddress(address: String) = cashAddr.isValidCashAddress(address)

    fun getPublicKeyHash(address: String): ByteArray {
        return cashAddr.decodeCashAddress(address).hash
    }
}