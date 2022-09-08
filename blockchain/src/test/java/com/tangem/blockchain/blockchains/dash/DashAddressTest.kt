package com.tangem.blockchain.blockchains.dash

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressType
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class DashAddressTest {

    private val addressService = BitcoinAddressService(Blockchain.Dash)

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "021DCF0C1E183089515DF8C86DACE6DA08DC8E1232EA694388E49C3C66EB79A418".hexToBytes()
        val expected = "yMfdoASh4QEM3zVpZqgXJ8St38X7VWnzp7"

        Truth.assertThat(addressService.makeAddress(walletPublicKey)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "yMfdoASh4QEM3zVpZqgXJ8St38X7VWnzp7"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }
}