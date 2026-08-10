package com.tangem.blockchain.blockchains.tron.network

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * The node returns the allowance in the token's smallest units. [TronNetworkService.getAllowance]
 * must convert it to human-readable units — the same contract EthereumLikeNetworkService fulfils —
 * because callers compare the result against user-entered amounts. Returning raw units makes any
 * non-zero allowance look practically unlimited (e.g. 5 USDT reads as 5_000_000).
 */
internal class TronAllowanceTest {

    private val provider: TronNetworkProvider = mockk {
        every { baseUrl } returns "https://api.tron/"
    }

    private val service = TronNetworkService(
        rpcNetworkProviders = listOf(provider),
        blockchain = Blockchain.Tron,
    )

    private val usdt = Token(
        symbol = "USDT",
        contractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
        decimals = 6,
    )

    @Test
    fun `GIVEN allowance in smallest units WHEN getAllowance THEN human units returned`() = runTest {
        // Arrange
        // 0x4C4B40 = 5_000_000 raw units = 5 USDT with 6 decimals
        stubAllowance("00000000000000000000000000000000000000000000000000000000004c4b40")

        // Act
        val actual = service.getAllowance("TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF", usdt, SPENDER)

        // Assert
        assertThat(actual.getOrNull()).isEquivalentAccordingToCompareTo(BigDecimal("5"))
    }

    @Test
    fun `GIVEN zero allowance WHEN getAllowance THEN zero returned`() = runTest {
        // Arrange
        stubAllowance("0000000000000000000000000000000000000000000000000000000000000000")

        // Act
        val actual = service.getAllowance("TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF", usdt, SPENDER)

        // Assert
        assertThat(actual.getOrNull()).isEquivalentAccordingToCompareTo(BigDecimal.ZERO)
    }

    @Test
    fun `GIVEN unlimited allowance WHEN getAllowance THEN max uint256 shifted by decimals`() = runTest {
        // Arrange
        val maxUint256 = BigDecimal(BigInteger("f".repeat(64), 16))
        stubAllowance("f".repeat(64))

        // Act
        val actual = service.getAllowance("TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF", usdt, SPENDER)

        // Assert
        assertThat(actual.getOrNull()).isEquivalentAccordingToCompareTo(maxUint256.movePointLeft(6))
    }

    private fun stubAllowance(constantResultHex: String) {
        coEvery {
            provider.getAllowance(any())
        } returns Result.Success(
            TronTriggerSmartContractResponse(
                constantResult = listOf(constantResultHex),
                energyUsed = 0,
            ),
        )
    }

    private companion object {
        const val SPENDER = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn"
    }
}