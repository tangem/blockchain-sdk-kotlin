package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplyStatusCallData]
 */
internal class EthereumYieldSupplyStatusCallDataTest {

    private val signature = "0xf8e8be9c".hexToBytes()
    private val tokenContractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"

    private val tokenAddressData = "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplyStatusCallData(
            tokenContractAddress = tokenContractAddress,
        )
        val expected = signature + tokenAddressData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }
}