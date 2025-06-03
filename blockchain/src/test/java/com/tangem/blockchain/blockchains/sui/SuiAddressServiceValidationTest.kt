package com.tangem.blockchain.blockchains.sui

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.sui.network.SuiConstants
import com.tangem.blockchain.common.Blockchain
import org.junit.Test

class SuiAddressServiceValidationTest {

    private val addressService = SuiAddressService(Blockchain.Sui)

    @Test
    fun validContractAddressTest() {
        val address = "0x06864a6f921804860930db6ddbe2e16acdf8504495ea7481637a1c8b9a8fe54b::cetus::CETUS"
        Truth.assertThat(addressService.validateContractAddress(address)).isTrue()
    }

    @Test
    fun validTrimmedObjectId() {
        val address = "0x6864a6f921804860930db6ddbe2e16acdf8504495ea7481637a1c8b9a8fe54b::cetus::CETUS"
        Truth.assertThat(addressService.validateContractAddress(address)).isTrue()
    }

    @Test
    fun validShortObjectId() {
        val address = "0x1234::mod::Token"
        Truth.assertThat(addressService.validateContractAddress(address)).isTrue()
    }

    @Test
    fun invalidHexInObjectIdShouldFail() {
        val address = "0xZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ::mod::Token"
        Truth.assertThat(addressService.validateContractAddress(address)).isFalse()
    }

    @Test
    fun missingPartsShouldFail() {
        val address = "0x06864a6f921804860930db6ddbe2e16acdf8504495ea7481637a1c8b9a8fe54b::cetus"
        Truth.assertThat(addressService.validateContractAddress(address)).isFalse()
    }

    @Test
    fun moduleNameWithIllegalCharsShouldFail() {
        val address = "0x06864a6f921804860930db6ddbe2e16acdf8504495ea7481637a1c8b9a8fe54b::ce-tus::CETUS"
        Truth.assertThat(addressService.validateContractAddress(address)).isFalse()
    }

    @Test
    fun structNameWithIllegalCharsShouldFail() {
        val address = "0x06864a6f921804860930db6ddbe2e16acdf8504495ea7481637a1c8b9a8fe54b::cetus::CE-TUS"
        Truth.assertThat(addressService.validateContractAddress(address)).isFalse()
    }

    @Test
    fun validCoinContractAddress() {
        val address = SuiConstants.COIN_TYPE
        Truth.assertThat(addressService.validateContractAddress(address)).isTrue()
    }
}