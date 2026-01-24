package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplySendCallData]
 */
internal class EthereumYieldSupplySendCallDataTest {
    private val blockchain = Blockchain.Ethereum

    private val signature = "0x0779afe6".hexToBytes()
    private val tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678"
    private val destinationAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
    private val amount = Amount(Blockchain.Ethereum).copy(
        value = "100".toBigDecimal(),
    )

    private val tokenAddressData = "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()
    private val destinationAddressData = "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd".hexToBytes()
    private val amountData = "0000000000000000000000000000000000000000000000056bc75e2d63100000".hexToBytes()

    @Test
    fun `Is call data is correct`() {
        val callData = EthereumYieldSupplySendCallData(
            tokenContractAddress = tokenContractAddress,
            destinationAddress = destinationAddress,
            amount = amount,
        )
        val expected = signature + tokenAddressData + destinationAddressData + amountData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `Validate call data`() {
        val validCallData = EthereumYieldSupplySendCallData(
            tokenContractAddress = tokenContractAddress,
            destinationAddress = destinationAddress,
            amount = amount,
        )
        Truth.assertThat(validCallData.validate(blockchain)).isTrue()

        val invalidCallData = EthereumYieldSupplySendCallData(
            tokenContractAddress = "",
            destinationAddress = destinationAddress,
            amount = amount,
        )
        Truth.assertThat(invalidCallData.validate(blockchain)).isFalse()

        val invalidCallData1 = EthereumYieldSupplySendCallData(
            tokenContractAddress = tokenContractAddress,
            destinationAddress = "",
            amount = amount,
        )
        Truth.assertThat(invalidCallData1.validate(blockchain)).isFalse()

        val invalidCallData3 = EthereumYieldSupplySendCallData(
            tokenContractAddress = EthereumUtils.ZERO_ADDRESS,
            destinationAddress = destinationAddress,
            amount = amount,
        )
        Truth.assertThat(invalidCallData3.validate(blockchain)).isFalse()

        val invalidCallData4 = EthereumYieldSupplySendCallData(
            tokenContractAddress = "0xG234567890123456789012345678901234567890",
            destinationAddress = destinationAddress,
            amount = amount,
        )
        Truth.assertThat(invalidCallData4.validate(blockchain)).isFalse()

        val invalidCallData5 = EthereumYieldSupplySendCallData(
            tokenContractAddress = tokenContractAddress,
            destinationAddress = EthereumUtils.ZERO_ADDRESS,
            amount = amount,
        )
        Truth.assertThat(invalidCallData5.validate(blockchain)).isFalse()

        val invalidCallData6 = EthereumYieldSupplySendCallData(
            tokenContractAddress = tokenContractAddress,
            destinationAddress = "0xG234567890123456789012345678901234567890",
            amount = amount,
        )
        Truth.assertThat(invalidCallData6.validate(blockchain)).isFalse()
    }
}