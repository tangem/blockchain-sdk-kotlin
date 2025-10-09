package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Test for [EthereumYieldSupplySendCallData]
 */
internal class EthereumYieldSupplySendCallDataTest {
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
}