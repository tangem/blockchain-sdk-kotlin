package com.tangem.blockchain.blockchains.hedera

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HederaUtilsAndAddressServiceTest {

    private val addressService = HederaAddressService(isTestnet = false)

    @Test
    fun evmAddressValidation_acceptsLowerAndUpperPrefix() {
        val lower = "0x1234567890abcdef1234567890abcdef12345678"
        val upper = "0X1234567890abcdef1234567890abcdef12345678"

        assertThat(HederaUtils.isValidEvmAddress(lower)).isTrue()
        assertThat(HederaUtils.isValidEvmAddress(upper)).isTrue()
    }

    @Test
    fun encodeBalanceOf_handlesUppercasePrefixWithoutCorruptingCalldata() {
        val encoded = HederaUtils.encodeBalanceOf("0X1234567890abcdef1234567890abcdef12345678")

        assertThat(encoded.startsWith("0x70a08231")).isTrue()
        assertThat(encoded.length).isEqualTo(74)
        assertThat(encoded.contains("x1234")).isFalse()
    }

    @Test
    fun reformatContractAddress_convertsZeroPrefixedEvmAddressToAccountId() {
        val evm = "0x00000000000000000000000000000000000004e2"

        assertThat(addressService.reformatContractAddress(evm)).isEqualTo("0.0.1250")
    }

    @Test
    fun validateContractAddress_rejectsInvalidEvmAddressLength() {
        assertThat(addressService.validateContractAddress("0x1234")).isFalse()
    }
}
