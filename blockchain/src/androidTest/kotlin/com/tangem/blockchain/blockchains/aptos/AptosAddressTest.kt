package com.tangem.blockchain.blockchains.aptos

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.TrustWalletAddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import junit.framework.TestCase.assertEquals
import org.junit.Test

internal class AptosAddressTest {

    private val addressService = TrustWalletAddressService(Blockchain.Aptos)

    init {
        System.loadLibrary("TrustWalletCore")
    }

    @Test
    fun test() {
        val walletPublicKey = "0x62e7a6a486553b56a53e89dfae3f780693e537e5b0a7ed33290780e581ca8369".hexToBytes()
        val expected = "0x1869b853768f0ba935d67f837a66b172dd39a60ca2315f8d4e0e669bbd35cf25"

        assertEquals(addressService.makeAddress(walletPublicKey, EllipticCurve.Ed25519Slip0010), expected)
    }
}
