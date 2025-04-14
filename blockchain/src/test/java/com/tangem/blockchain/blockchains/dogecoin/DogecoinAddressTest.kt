package com.tangem.blockchain.blockchains.dogecoin

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class DogecoinAddressTest {

    private val addressService = BitcoinAddressService(Blockchain.Dogecoin)

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = (
            "04BE37CD5251C8999EDBBFC759D800EB41E4DCB718289601EB15819404E1B2F2ED90FE50C2A481D06EC790D1EF6184974EB655A" +
                "BAE4BE56A6D1C9E1A17B1EFDF02"
            ).hexToBytes()
        val expected = "DRgF4iLXRhnYeQEV9kHmkvvnz128uCFZXL"

        Truth.assertThat(addressService.makeAddress(walletPublicKey))
            .isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "DRgF4iLXRhnYeQEV9kHmkvvnz128uCFZXL"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }
}
