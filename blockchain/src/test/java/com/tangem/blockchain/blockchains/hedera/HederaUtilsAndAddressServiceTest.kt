package com.tangem.blockchain.blockchains.hedera

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HederaUtilsAndAddressServiceTest {

    private val addressService = HederaAddressService(isTestnet = false)
    private val converter = HederaTokenAddressConverter()

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

    @Test
    fun resolveTokenId_convertsZeroPrefixLocally() = runTest {
        val evm = "0x00000000000000000000000000000000000004e2"

        val result = converter.resolveTokenId(evm) { error("should not be called") }

        assertThat(result).isEqualTo("0.0.1250")
    }

    @Test
    fun resolveTokenId_resolvesNonZeroPrefixViaNetwork() = runTest {
        val evm = "0x00a63cab099ad6e9322f0e4b69e5bedaf94563dd"

        val result = converter.resolveTokenId(evm) { "0.0.1380220" }

        assertThat(result).isEqualTo("0.0.1380220")
    }

    @Test
    fun resolveTokenId_fallsBackToEvmOnNetworkFailure() = runTest {
        val evm = "0x00a63cab099ad6e9322f0e4b69e5bedaf94563dd"

        val result = converter.resolveTokenId(evm) { null }

        assertThat(result).isEqualTo(evm)
    }

    @Test
    fun resolveTokenId_passesThrough0dot0Format() = runTest {
        val result = converter.resolveTokenId("0.0.1250") { error("should not be called") }

        assertThat(result).isEqualTo("0.0.1250")
    }
}