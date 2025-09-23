package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplyEffectiveProtocolBalanceCallData]
 */
internal class EthereumYieldSupplyEffectiveProtocolBalanceCallDataTest {

    private val signature = "0x5002bb7e".hexToBytes()
    private val tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678"

    private val tokenAddressData = "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplyEffectiveProtocolBalanceCallData(
            tokenContractAddress = tokenContractAddress,
        )
        val expected = signature + tokenAddressData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }
}