package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import java.math.BigDecimal

internal class EthereumYieldSupplyWithdrawCallDataTest {
    private val blockchain = Blockchain.Ethereum
    private val methodId = "0xf3fef3a3".hexToBytes()
    private val tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678"
    private val tokenAddressData = "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()

    private val amount = Amount(
        token = Token(symbol = "USDC", contractAddress = tokenContractAddress, decimals = 6),
        value = BigDecimal("0.01"),
    )
    private val amountData = "0000000000000000000000000000000000000000000000000000000000002710".hexToBytes()

    @Test
    fun `call data is correct`() {
        val callData = EthereumYieldSupplyWithdrawCallData(
            tokenContractAddress = tokenContractAddress,
            amount = amount,
        )
        val expected = methodId + tokenAddressData + amountData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `validate call data`() {
        val valid = EthereumYieldSupplyWithdrawCallData(tokenContractAddress, amount)
        Truth.assertThat(valid.validate(blockchain)).isTrue()

        val zeroAddr = EthereumYieldSupplyWithdrawCallData(EthereumUtils.ZERO_ADDRESS, amount)
        Truth.assertThat(zeroAddr.validate(blockchain)).isFalse()

        val emptyAddr = EthereumYieldSupplyWithdrawCallData("", amount)
        Truth.assertThat(emptyAddr.validate(blockchain)).isFalse()

        val malformedAddr = EthereumYieldSupplyWithdrawCallData("0xG234567890123456789012345678901234567890", amount)
        Truth.assertThat(malformedAddr.validate(blockchain)).isFalse()
    }
}