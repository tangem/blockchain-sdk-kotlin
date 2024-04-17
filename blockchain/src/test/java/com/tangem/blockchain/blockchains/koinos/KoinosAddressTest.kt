package com.tangem.blockchain.blockchains.koinos

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class KoinosAddressTest {

    val addressService = KoinosAddressService()

    @Test
    fun address() {
        val publicKey = "03B2D98CF41E82D9B99842A1D05860A1B06532015138F9067239706E06EE38E621".hexToBytes()
        val expectedAddress = "1AYz8RCnoafLnifMjJbgNb2aeW5CbZj8Tp"

        Truth.assertThat(addressService.makeAddress(publicKey))
            .isEqualTo(expectedAddress)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "1AYz8RCnoafLnifMjJbgNb2aeW5CbZj8Tp"

        Truth.assertThat(addressService.validate(address))
            .isTrue()
    }

    @Test
    fun validateIncorrectAddress() {
        val address = "1AYz8RCnoafLnifMjJbgNb2aeW5CbZj8T"

        Truth.assertThat(addressService.validate(address))
            .isFalse()
    }
}