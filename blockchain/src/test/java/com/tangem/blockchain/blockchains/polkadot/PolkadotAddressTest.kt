package com.tangem.blockchain.blockchains.polkadot

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class PolkadotAddressTest {

    private val polkadotAddressService = PolkadotAddressService(Blockchain.Polkadot)
    private val westendAddressService = PolkadotAddressService(Blockchain.PolkadotTestnet)

    private val polkadotAddress = "12RdYdDR4pwARe1j57rXkcxFxNuwDc29V59HdUvVMYBFYw9X"
    private val westendAddress = "5DVLQHxMD3fgz71D7UoXcU876kvHXJU1QaQoUBw8oT9jNfvx"

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "3F05253ACDDB17A527BA6E9DBD73E7B06FFCF7CE072041052BEF31B6ECBD7CE2".hexToBytes()
        Truth.assertThat(polkadotAddressService.makeAddress(walletPublicKey)).isEqualTo(polkadotAddress)
        Truth.assertThat(westendAddressService.makeAddress(walletPublicKey)).isEqualTo(westendAddress)
    }

    @Test
    fun validateCorrectAddress() {
        Truth.assertThat(polkadotAddressService.validate(polkadotAddress)).isTrue()
        Truth.assertThat(westendAddressService.validate(westendAddress)).isTrue()
    }
}