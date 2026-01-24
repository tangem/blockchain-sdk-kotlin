package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplyExitCallData]
 */
internal class EthereumYieldSupplyExitCallDataTest {
    private val blockchain = Blockchain.Ethereum

    private val signature = "0xc65e6dcf".hexToBytes()
    private val tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678"

    private val tokenAddressData = "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplyExitCallData(
            tokenContractAddress = tokenContractAddress,
        )
        val expected = signature + tokenAddressData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `WHEN correct data THEN return RESULT`() {
        val rawCallData = "0xc65e6dcf0000000000000000000000001234567890abcdef1234567890abcdef12345678"

        val actual = EthereumYieldSupplyExitCallData.decode(rawCallData)
        val expected = EthereumYieldSupplyExitCallData(
            tokenContractAddress = tokenContractAddress,
        )

        Truth.assertThat(actual).isNotNull()
        Truth.assertThat(actual!!.tokenContractAddress).isEqualTo(expected.tokenContractAddress)
        Truth.assertThat(actual.data).isEqualTo(expected.data)
    }

    @Test
    fun `WHEN error data THEN return NULL`() {
        val rawCallData = "0xc65e6dcf000000000000000000000000123456abcdef1234567890csabcdef12345678"

        val actual = EthereumYieldSupplyExitCallData.decode(rawCallData)

        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `WHEN wrong method data THEN return NULL`() {
        val rawCallData = "0xc6526dcf0000000000000000000000001234567890abcdef1234567890abcdef12345678"

        val actual = EthereumYieldSupplyExitCallData.decode(rawCallData)

        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `Validate call data`() {
        val validCallData = EthereumYieldSupplyExitCallData(
            tokenContractAddress = tokenContractAddress,
        )
        Truth.assertThat(validCallData.validate(blockchain)).isTrue()

        val invalidCallData = EthereumYieldSupplyExitCallData(
            tokenContractAddress = "",
        )
        Truth.assertThat(invalidCallData.validate(blockchain)).isFalse()

        val invalidCallData1 = EthereumYieldSupplyExitCallData(
            tokenContractAddress = "0xG234567890123456789012345678901234567890",
        )
        Truth.assertThat(invalidCallData1.validate(blockchain)).isFalse()

        val invalidCallData2 = EthereumYieldSupplyExitCallData(
            tokenContractAddress = EthereumUtils.ZERO_ADDRESS,
        )
        Truth.assertThat(invalidCallData2.validate(blockchain)).isFalse()
    }
}