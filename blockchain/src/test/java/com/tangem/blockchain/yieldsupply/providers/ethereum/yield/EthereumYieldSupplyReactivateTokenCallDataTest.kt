package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import java.math.BigDecimal

/**
 * Test for [EthereumYieldSupplyReactivateTokenCallData]
 */
internal class EthereumYieldSupplyReactivateTokenCallDataTest {
    private val blockchain = Blockchain.Ethereum

    private val signature = "0xc478e956".hexToBytes()
    private val tokenContractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
    private val maxNetworkFee = Amount(Blockchain.Ethereum).copy(
        value = "100".toBigDecimal(),
    )

    private val tokenAddressData = "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd".hexToBytes()
    private val maxNetworkFeeData = "0000000000000000000000000000000000000000000000056bc75e2d63100000".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplyReactivateTokenCallData(
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,

        )
        val expected = signature + tokenAddressData + maxNetworkFeeData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `WHEN correct data THEN return RESULT`() {
        val rawCallData = "0xc478e956000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd" +
            "0000000000000000000000000000000000000000000000056bc75e2d63100000"

        val actual = EthereumYieldSupplyReactivateTokenCallData.decode(rawCallData)
        val expected = EthereumYieldSupplyReactivateTokenCallData(
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )

        Truth.assertThat(actual).isNotNull()
        Truth.assertThat(actual!!.tokenContractAddress).isEqualTo(expected.tokenContractAddress)
    }

    @Test
    fun `WHEN error data THEN return NULL`() {
        val rawCallData = "0xc478e95600000000000000000000000a2defabcdefabcdefabcdefabcdefabcdefabcd" +
            "000000000000000000000000000000000000000000000005c75e2d63100000"

        val actual = EthereumYieldSupplyReactivateTokenCallData.decode(rawCallData)

        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `WHEN wrong method data THEN return NULL`() {
        val rawCallData = "0xc478e9520000000000000000000000001234567890abcdef1234567890abcdef12345678" +
            "000000000000000000000000000000000000000000000005c75e2d63100000"

        val actual = EthereumYieldSupplyReactivateTokenCallData.decode(rawCallData)

        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `Validate call data`() {
        val validCallData = EthereumYieldSupplyReactivateTokenCallData(
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(validCallData.validate(blockchain)).isTrue()

        val invalidCallData = EthereumYieldSupplyReactivateTokenCallData(
            tokenContractAddress = "",
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData.validate(blockchain)).isFalse()

        val invalidCallData1 = EthereumYieldSupplyReactivateTokenCallData(
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = Amount(Blockchain.Ethereum).copy(value = BigDecimal.ZERO),
        )
        Truth.assertThat(invalidCallData1.validate(blockchain)).isFalse()

        val invalidCallData2 = EthereumYieldSupplyReactivateTokenCallData(
            tokenContractAddress = "0xG234567890123456789012345678901234567890",
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData2.validate(blockchain)).isFalse()

        val invalidCallData3 = EthereumYieldSupplyReactivateTokenCallData(
            tokenContractAddress = EthereumUtils.ZERO_ADDRESS,
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData3.validate(blockchain)).isFalse()
    }
}