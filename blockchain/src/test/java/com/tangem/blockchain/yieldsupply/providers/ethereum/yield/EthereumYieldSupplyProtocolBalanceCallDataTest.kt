package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplyProtocolBalanceCallData]
 */
internal class EthereumYieldSupplyProtocolBalanceCallDataTest {

    private val signature = "0x4bd22a1b".hexToBytes()
    private val tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678"

    private val tokenAddressData = "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplyProtocolBalanceCallData(
            tokenContractAddress = tokenContractAddress,
        )
        val expected = signature + tokenAddressData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }
}