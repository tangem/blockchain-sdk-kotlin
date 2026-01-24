package com.tangem.blockchain.yieldsupply.providers.ethereum.factory

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import java.math.BigDecimal

/**
 * Test for [EthereumYieldSupplyDeployCallData]
 */
internal class EthereumYieldSupplyDeployCallDataTest {
    private val blockchain = Blockchain.Ethereum

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

    @Test
    fun `Validate call data`() {
        val validCallData = EthereumYieldSupplyDeployCallData(
            address = walletAddress,
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(validCallData.validate(blockchain)).isTrue()

        val invalidCallData = EthereumYieldSupplyDeployCallData(
            address = "",
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData.validate(blockchain)).isFalse()

        val invalidCallData1 = EthereumYieldSupplyDeployCallData(
            address = walletAddress,
            tokenContractAddress = "",
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData1.validate(blockchain)).isFalse()

        val invalidCallData2 = EthereumYieldSupplyDeployCallData(
            address = walletAddress,
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = Amount(Blockchain.Ethereum).copy(value = BigDecimal.ZERO),
        )
        Truth.assertThat(invalidCallData2.validate(blockchain)).isFalse()

        val invalidCallData3 = EthereumYieldSupplyDeployCallData(
            address = EthereumUtils.ZERO_ADDRESS,
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData3.validate(blockchain)).isFalse()

        val invalidCallData4 = EthereumYieldSupplyDeployCallData(
            address = walletAddress,
            tokenContractAddress = EthereumUtils.ZERO_ADDRESS,
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData4.validate(blockchain)).isFalse()

        val invalidCallData5 = EthereumYieldSupplyDeployCallData(
            address = "0xG234567890123456789012345678901234567890",
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData5.validate(blockchain)).isFalse()

        val invalidCallData6 = EthereumYieldSupplyDeployCallData(
            address = walletAddress,
            tokenContractAddress = "0xG234567890123456789012345678901234567890",
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData6.validate(blockchain)).isFalse()
    }
}