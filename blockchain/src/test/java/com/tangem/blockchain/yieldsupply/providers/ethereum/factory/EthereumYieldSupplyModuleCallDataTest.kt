package com.tangem.blockchain.yieldsupply.providers.ethereum.factory

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplyModuleCallData]
 */
internal class EthereumYieldSupplyModuleCallDataTest {

    private val signature = "0x36571e2c".hexToBytes()
    private val walletAddress = "0x1234567890abcdef1234567890abcdef12345678"

    private val walletAddressData = "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplyModuleCallData(
            address = walletAddress,
        )
        val expected = signature + walletAddressData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }
}