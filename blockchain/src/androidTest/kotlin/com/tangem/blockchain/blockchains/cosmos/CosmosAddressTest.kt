package com.tangem.blockchain.blockchains.cosmos

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.TrustWalletAddressService
import com.tangem.common.extensions.hexToBytes
import junit.framework.TestCase.assertEquals
import org.junit.Test

class CosmosAddressTest {

    private val addressService = TrustWalletAddressService(Blockchain.Cosmos)

    init {
        System.loadLibrary("TrustWalletCore")
    }

    @Test
    fun testAddressValidation1() {
        val walletPublicKey = "038C869947BB27B78D36F2D60158CAF295EA874943D9574DC9C2228DB152D9A1C2".hexToBytes()
        val expected = "cosmos1edt8c8lqghvj94tcqy3yc8afy0xyv2563q0nhm"

        assertEquals(addressService.makeAddress(walletPublicKey), expected)
    }

    @Test
    fun testAddressValidation2() {
        val walletPublicKey = "02D8BC21B99355D87B16C3D82F1790D04AACD557B538A3A72139EBF4E1476F311A".hexToBytes()
        val expected = "cosmos1tqksn8j4kj0feed2sglhfujp5amkndyac4z8jy"

        assertEquals(addressService.makeAddress(walletPublicKey), expected)
    }
}
