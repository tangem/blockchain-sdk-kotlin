package com.tangem.blockchain.blockchains.filecoin

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.WalletCoreAddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toDecompressedPublicKey
import junit.framework.TestCase.assertEquals
import org.junit.Test

internal class FilecoinAddressTest {

    private val addressService = WalletCoreAddressService(Blockchain.Filecoin)

    init {
        System.loadLibrary("TrustWalletCore")
    }

    @Test
    fun test() {
        val walletPublicKey = "038A3F02BEBAFD04C1FA82184BA3950C801015A0B61A0922110D7CEE42A2A13763".hexToBytes()
            .toDecompressedPublicKey()

        val expected = "f1hbyibpq4mea6l3no7aag24hxpwgf4zwp6msepwi"

        assertEquals(addressService.makeAddress(walletPublicKey, EllipticCurve.Secp256k1), expected)
    }
}