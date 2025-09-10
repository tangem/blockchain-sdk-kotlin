package com.tangem.blockchain.yieldsupply.providers.ethereum.factory

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplyDeployCallData]
 */
internal class EthereumYieldSupplyDeployCallDataTest {

    private val signature = "0xcbeda14c".hexToBytes()
    private val walletAddress = "0x1234567890abcdef1234567890abcdef12345678"
    private val tokenContractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
    private val maxNetworkFee = Amount(Blockchain.Ethereum).copy(
        value = "100".toBigDecimal(),
    )

    private val walletAddressData = "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()
    private val tokenAddressData = "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd".hexToBytes()
    private val maxNetworkFeeData = "0000000000000000000000000000000000000000000000056bc75e2d63100000".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplyDeployCallData(
            address = walletAddress,
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )
        val expected = signature + walletAddressData + tokenAddressData + maxNetworkFeeData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }
}