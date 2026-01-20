package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplyEnterCallData]
 */
internal class EthereumYieldSupplyEnterCallDataTest {

    private val signature = "0x79be55f7".hexToBytes()
    private val tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678"

    private val tokenAddressData = "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplyEnterCallData(
            tokenContractAddress = tokenContractAddress,
        )
        val expected = signature + tokenAddressData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `WHEN correct data THEN return RESULT`() {
        val rawCallData = "0x79be55f70000000000000000000000001234567890abcdef1234567890abcdef12345678"

        val actual = EthereumYieldSupplyEnterCallData.decode(rawCallData)
        val expected = EthereumYieldSupplyEnterCallData(tokenContractAddress = tokenContractAddress)

        Truth.assertThat(actual).isNotNull()
        Truth.assertThat(actual!!.tokenContractAddress).isEqualTo(expected.tokenContractAddress)
    }

    @Test
    fun `WHEN error data THEN return NULL`() {
        val rawCallData = "0x79be55f7000000000000000000000001234m67890abcdef1234567890abcdef1234567p"

        val actual = EthereumYieldSupplyEnterCallData.decode(rawCallData)

        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `WHEN wrong method data THEN return NULL`() {
        val rawCallData = "0xc6526dcf0000000000000000000000001234567890abcdef1234567890abcdef12345678"

        val actual = EthereumYieldSupplyEnterCallData.decode(rawCallData)

        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `Validate call data`() {
        val validCallData = EthereumYieldSupplyEnterCallData(
            tokenContractAddress = tokenContractAddress,
        )
        Truth.assertThat(validCallData.validate()).isTrue()

        val invalidCallData = EthereumYieldSupplyEnterCallData(
            tokenContractAddress = "",
        )
        Truth.assertThat(invalidCallData.validate()).isFalse()

        val invalidCallData1 = EthereumYieldSupplyEnterCallData(
            tokenContractAddress = "0xG234567890123456789012345678901234567890",
        )
        Truth.assertThat(invalidCallData1.validate()).isFalse()

        val invalidCallData2 = EthereumYieldSupplyEnterCallData(
            tokenContractAddress = EthereumUtils.ZERO_ADDRESS,
        )
        Truth.assertThat(invalidCallData2.validate()).isFalse()
    }
}