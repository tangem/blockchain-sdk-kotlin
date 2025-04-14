package com.tangem.blockchain.blockchains.koinos

import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toCompressedPublicKey

internal class KoinosAddressService : AddressService() {

    private val bitcoinAddressService = BitcoinAddressService(Blockchain.Bitcoin)

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return bitcoinAddressService.makeLegacyAddress(walletPublicKey.toCompressedPublicKey()).value
    }

    override fun validate(address: String): Boolean {
        return bitcoinAddressService.validateLegacyAddress(address)
    }
}