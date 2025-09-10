package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplyInitTokenCallData]
 */
internal class EthereumYieldSupplyInitTokenCallDataTest {

    private val signature = "0xebd4b81c".hexToBytes()
    private val tokenContractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
    private val maxNetworkFee = Amount(Blockchain.Ethereum).copy(
        value = "100".toBigDecimal(),
    )

    private val tokenAddressData = "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd".hexToBytes()
    private val maxNetworkFeeData = "0000000000000000000000000000000000000000000000056bc75e2d63100000".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplyInitTokenCallData(
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )
        val expected = signature + tokenAddressData + maxNetworkFeeData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }
}