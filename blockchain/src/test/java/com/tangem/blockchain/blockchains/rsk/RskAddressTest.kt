package com.tangem.blockchain.blockchains.rsk

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class RskAddressTest {

    private val addressService = RskAddressService()

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = (
            "04BAEC8CD3BA50FDFE1E8CF2B04B58E17041245341CD1F1C6B3A496B48956DB4C896A6848BCF8FCFC33B88341507DD25E5F4609" +
                "386C68086C74CF472B86E5C3820"
            ).hexToBytes()
        val expected = "0xc63763572D45171E4C25cA0818B44e5DD7f5c15b"

        Truth.assertThat(addressService.makeAddress(walletPublicKey)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "0xc63763572d45171e4c25ca0818b44e5dd7f5c15b"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateCorrectAddressWithChecksum() {
        val address = "0xc63763572D45171E4C25cA0818B44e5DD7f5c15b"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }
}
