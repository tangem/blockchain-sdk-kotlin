package com.tangem.blockchain.blockchains.dash

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class DashAddressTest {

    private val addressService = BitcoinAddressService(Blockchain.Dash)

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = (
            "0441DCD64B5F4A039FC339A16300A833A883B218909F2EBCAF3906651C76842C45E3D67E8D2947E6FEE8B62D3D3B6A4D5F212DA" +
                "23E478DD69A2C6CCC851F300D80"
            ).hexToBytes()
        val expected = "Xs92pJsKUXRpbwzxDjBjApiwMK6JysNntG"

        Truth.assertThat(addressService.makeAddress(walletPublicKey)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "Xs92pJsKUXRpbwzxDjBjApiwMK6JysNntG"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }
}
