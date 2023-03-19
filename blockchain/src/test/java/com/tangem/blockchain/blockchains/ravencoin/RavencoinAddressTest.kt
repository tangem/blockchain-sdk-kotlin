package com.tangem.blockchain.blockchains.ravencoin

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class RavencoinAddressTest {

    private val addressService = BitcoinAddressService(Blockchain.Ravencoin)

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "0241DCD64B5F4A039FC339A16300A833A883B218909F2EBCAF3906651C76842C45".hexToBytes()
        val expected = "RT1iM3xbqSQ276GNGGNGFdYrMTEeq4hXRH"

        Truth.assertThat(addressService.makeAddress(walletPublicKey)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "RT1iM3xbqSQ276GNGGNGFdYrMTEeq4hXRH"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }
}