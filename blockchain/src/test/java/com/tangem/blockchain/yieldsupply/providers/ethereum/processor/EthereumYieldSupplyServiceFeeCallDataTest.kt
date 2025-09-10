package com.tangem.blockchain.yieldsupply.providers.ethereum.processor

import com.google.common.truth.Truth
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyModuleCallData
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplyModuleCallData]
 */
internal class EthereumYieldSupplyServiceFeeCallDataTest {

    private val signature = "0x61d1bc94".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplyServiceFeeCallData

        val expected = signature
        Truth.assertThat(callData.data).isEqualTo(expected)
    }
}