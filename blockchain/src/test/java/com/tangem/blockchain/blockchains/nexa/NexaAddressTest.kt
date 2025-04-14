package com.tangem.blockchain.blockchains.nexa

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class NexaAddressTest {

    val addressService = NexaAddressService(isTestNet = false)

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "029a1fa7122e910b1337492d88a6f906ab5ced9a8d7e7e3185e7dc5c5603f94e0d".hexToBytes()
        val expected = "nexa:nqtsq5g5whssewa9ewt0g8spdu0x6rz7g526w0dut3rntrp9"

        Truth.assertThat(addressService.makeAddress(walletPublicKey))
            .isEqualTo(expected)
    }

    @Test
    fun validateCorrectTEMPLATEAAddress() {
        val address = "nexa:nqtsq5g5xwpr2kp6fvhng5lyuafu8jg3gaj8hk34k2r4rvs3"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateCorrectP2PKHAddress() {
        val address = "nexa:qz2e2eesqa4axqm7rtej0nnt6sq6t2y33cu3e7ss2w"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateIncorrectTemplateAAddress() {
        val address = "nexa:zw2e2eesqa4axqm7rtej0nnt6sq6t2y33c2e90h9ma"

        Truth.assertThat(addressService.validate(address)).isFalse()
    }

    @Test
    fun getCorrectTemplateAddressPublicKey() {
        val address = "nexa:nqtsq5g5whssewa9ewt0g8spdu0x6rz7g526w0dut3rntrp9"
        val expected = "1700511475e10cbba5cb96f41e016f1e6d0c5e4515a73dbc".hexToBytes()

        Truth.assertThat(addressService.getPublicKey(address)).isEqualTo(expected)
    }

    @Test
    fun getCorrectTemplateExplorerAddressPublicKey() {
        val address = "nexa:nqtsq5g5whssewa9ewt0g8spdu0x6rz7g526w0dut3rntrp9"
        val expected = "00511475e10cbba5cb96f41e016f1e6d0c5e4515a73dbc".hexToBytes()

        Truth.assertThat(addressService.getScriptPublicKey(address)).isEqualTo(expected)
    }

    @Test
    fun getCorrectTemplateAddressScripthash() {
        val walletPublicKey = "029a1fa7122e910b1337492d88a6f906ab5ced9a8d7e7e3185e7dc5c5603f94e0d".hexToBytes()
        val expected = "00c3ed718203572fd774f61fa6aeb20e7424bd7067908df7707d1e70909d7971"

        Truth.assertThat(NexaAddressService.getScriptHash(walletPublicKey).lowercase()).isEqualTo(expected)
    }
}