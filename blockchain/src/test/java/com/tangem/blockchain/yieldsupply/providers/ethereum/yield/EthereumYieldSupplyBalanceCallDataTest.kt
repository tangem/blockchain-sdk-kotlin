package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplyBalanceCallData]
 */
internal class EthereumYieldSupplyBalanceCallDataTest {

    private val signature = "0x16a398f7".hexToBytes()
    private val tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678"

    private val tokenAddressData = "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplyBalanceCallData(
            tokenContractAddress = tokenContractAddress,
        )
        val expected = signature + tokenAddressData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `Validate call data`() {
        val validCallData = EthereumYieldSupplyBalanceCallData(
            tokenContractAddress = tokenContractAddress,
        )
        Truth.assertThat(validCallData.validate()).isTrue()

        val invalidCallData = EthereumYieldSupplyBalanceCallData(
            tokenContractAddress = "",
        )
        Truth.assertThat(invalidCallData.validate()).isFalse()

        val invalidCallData1 = EthereumYieldSupplyBalanceCallData(
            tokenContractAddress = "0xG234567890123456789012345678901234567890",
        )
        Truth.assertThat(invalidCallData1.validate()).isFalse()

        val invalidCallData2 = EthereumYieldSupplyBalanceCallData(
            tokenContractAddress = EthereumUtils.ZERO_ADDRESS,
        )
        Truth.assertThat(invalidCallData2.validate()).isFalse()
    }
}