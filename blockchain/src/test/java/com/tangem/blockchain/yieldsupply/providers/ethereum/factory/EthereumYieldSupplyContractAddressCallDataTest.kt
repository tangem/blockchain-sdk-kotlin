package com.tangem.blockchain.yieldsupply.providers.ethereum.factory

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 *  Test for [EthereumYieldSupplyContractAddressCallData]
 */
internal class EthereumYieldSupplyContractAddressCallDataTest {

    private val signature = "0xebd6d0a4".hexToBytes()
    private val walletAddress = "0x1234567890abcdef1234567890abcdef12345678"
    private val spenderData = "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val address = walletAddress
        val callData = EthereumYieldSupplyContractAddressCallData(address)
        val expected = signature + spenderData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }
}