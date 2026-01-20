package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import java.math.BigDecimal

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

    @Test
    fun `WHEN correct data THEN return RESULT`() {
        val rawCallData = "0xebd4b81c000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd" +
            "0000000000000000000000000000000000000000000000056bc75e2d63100000"

        val actual = EthereumYieldSupplyInitTokenCallData.decode(rawCallData)
        val expected = EthereumYieldSupplyInitTokenCallData(
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )

        Truth.assertThat(actual).isNotNull()
        Truth.assertThat(actual!!.tokenContractAddress).isEqualTo(expected.tokenContractAddress)
    }

    @Test
    fun `WHEN error data THEN return NULL`() {
        val rawCallData = "0xebd4b81c00000000000000000000000a2defabcdefabcdefabcdefabcdefabcdefabcd" +
            "000000000000000000000000000000000000000000000005c75e2d63100000"

        val actual = EthereumYieldSupplyInitTokenCallData.decode(rawCallData)

        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `WHEN wrong method data THEN return NULL`() {
        val rawCallData = "0xc6526dcf0000000000000000000000001234567890abcdef1234567890abcdef12345678" +
            "000000000000000000000000000000000000000000000005c75e2d63100000"

        val actual = EthereumYieldSupplyInitTokenCallData.decode(rawCallData)

        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `Validate call data`() {
        val validCallData = EthereumYieldSupplyInitTokenCallData(
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(validCallData.validate()).isTrue()

        val invalidCallData = EthereumYieldSupplyInitTokenCallData(
            tokenContractAddress = "",
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData.validate()).isFalse()

        val invalidCallData1 = EthereumYieldSupplyInitTokenCallData(
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = Amount(Blockchain.Ethereum).copy(value = BigDecimal.ZERO),
        )
        Truth.assertThat(invalidCallData1.validate()).isFalse()

        val invalidCallData2 = EthereumYieldSupplyInitTokenCallData(
            tokenContractAddress = "0xG234567890123456789012345678901234567890",
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData2.validate()).isFalse()

        val invalidCallData3 = EthereumYieldSupplyInitTokenCallData(
            tokenContractAddress = EthereumUtils.ZERO_ADDRESS,
            maxNetworkFee = maxNetworkFee,
        )
        Truth.assertThat(invalidCallData3.validate()).isFalse()
    }
}