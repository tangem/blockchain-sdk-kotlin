package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplyStatusCallData]
 */
internal class EthereumYieldSupplyStatusCallDataTest {
    private val blockchain = Blockchain.Ethereum

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

    @Test
    fun `Validate call data`() {
        val validCallData = EthereumYieldSupplyStatusCallData(
            tokenContractAddress = tokenContractAddress,
        )
        Truth.assertThat(validCallData.validate(blockchain)).isTrue()

        val invalidCallData = EthereumYieldSupplyStatusCallData(
            tokenContractAddress = "",
        )
        Truth.assertThat(invalidCallData.validate(blockchain)).isFalse()

        val invalidCallData1 = EthereumYieldSupplyStatusCallData(
            tokenContractAddress = "0xG234567890123456789012345678901234567890",
        )
        Truth.assertThat(invalidCallData1.validate(blockchain)).isFalse()

        val invalidCallData2 = EthereumYieldSupplyStatusCallData(
            tokenContractAddress = EthereumUtils.ZERO_ADDRESS,
        )
        Truth.assertThat(invalidCallData2.validate(blockchain)).isFalse()
    }
}