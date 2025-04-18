package com.tangem.blockchain.blockchains.sui

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.sui.network.SuiConstants
import com.tangem.blockchain.common.Blockchain
import org.junit.Test

class SuiAddressServiceTest {

    private val addressService = SuiAddressService(Blockchain.Sui)

    private val expectedTokenAddress =
        "0x06864a6f921804860930db6ddbe2e16acdf8504495ea7481637a1c8b9a8fe54b::cetus::CETUS"

    @Test
    fun fullContractAddressTest() {
        val fullAddress = "0x06864a6f921804860930db6ddbe2e16acdf8504495ea7481637a1c8b9a8fe54b::cetus::CETUS"
        val actualAddress = addressService.reformatContractAddress(fullAddress)
        Truth.assertThat(actualAddress).isEqualTo(expectedTokenAddress)
    }

    @Test
    fun trimmedContractAddressTest() {
        val fullAddress = "0x6864a6f921804860930db6ddbe2e16acdf8504495ea7481637a1c8b9a8fe54b::cetus::CETUS"
        val actualAddress = addressService.reformatContractAddress(fullAddress)

        Truth.assertThat(actualAddress).isEqualTo(expectedTokenAddress)
    }

    @Test
    fun coinContractAddressTest() {
        val coinAddress = SuiConstants.COIN_TYPE
        val actualAddress = addressService.reformatContractAddress(coinAddress)
        Truth.assertThat(actualAddress).isEqualTo(coinAddress)
    }
}