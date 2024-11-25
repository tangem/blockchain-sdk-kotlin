package com.tangem.blockchain.blockchains.factorn

import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve

internal class Fact0rnAddressService : AddressService() {

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        TODO("Not yet implemented")
    }

    override fun validate(address: String): Boolean {
        TODO("Not yet implemented")
    }
}