package com.tangem.blockchain.blockchains.nexa

import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.BitcoinCashAddressType
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.BitcoinCashLikeAddressPrefix
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.CashAddr
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey

class NexaAddressService(
    blockchain: Blockchain,
) : AddressService() {

    private val cashAddr = when (blockchain) {
        Blockchain.Nexa -> CashAddr(BitcoinCashLikeAddressPrefix.Nexa)
        Blockchain.NexaTestnet -> CashAddr(BitcoinCashLikeAddressPrefix.NexaTestnet)
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }
    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?) =
        makeCashAddrAddress(walletPublicKey).value

    override fun validate(address: String) = cashAddr.isValidCashAddress(address)

    private fun makeCashAddrAddress(walletPublicKey: ByteArray): Address {
        val publicKeyHash = walletPublicKey.toCompressedPublicKey().calculateSha256().calculateRipemd160()
        val address = cashAddr.toCashAddress(BitcoinCashAddressType.P2PKH, publicKeyHash)
        return Address(address, AddressType.Default)
    }

    fun getPublicKeyHash(address: String): ByteArray {
        return cashAddr.decodeCashAddress(address).hash
    }
}