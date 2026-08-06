package com.tangem.blockchain.blockchains.solana

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

internal class SolanaScaledUiAmountMultiplierResolverTest {

    private var hostCounter = 0

    @Test
    fun `GIVEN two providers report the same multiplier WHEN resolve THEN value is returned`() = runTest {
        // Arrange
        val resolver = createResolver(
            provider(BigDecimal("1.5")),
            provider(BigDecimal("1.5")),
            provider(BigDecimal("0.001")),
        )

        // Act
        val result = resolver.resolve(MINT)

        // Assert
        assertThat((result as Result.Success).data).isEqualTo(BigDecimal("1.5"))
    }

    @Test
    fun `GIVEN providers report the same multiplier in different scale WHEN resolve THEN value is returned`() =
        runTest {
            val resolver = createResolver(provider(BigDecimal("0.5")), provider(BigDecimal("0.50")))

            val result = resolver.resolve(MINT)

            assertThat((result as Result.Success).data).isEqualTo(BigDecimal("0.5"))
        }

    @Test
    fun `GIVEN one provider understates the multiplier WHEN resolve THEN transaction is stopped`() = runTest {
        // Arrange
        // The report's scenario: a hostile provider reports 0.001 while the honest one reports 1.0
        val resolver = createResolver(provider(BigDecimal("0.001")), provider(BigDecimal.ONE))

        // Act
        val result = resolver.resolve(MINT)

        // Assert
        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Solana.ScaledUiAmountMultiplierMismatch)
    }

    @Test
    fun `GIVEN one provider drops the extension WHEN resolve THEN transaction is stopped`() = runTest {
        // Arrange
        // Omitting scaledUiAmountConfig understates the multiplier down to a neutral one, so it is a vote too
        val resolver = createResolver(provider(null), provider(BigDecimal("5")))

        // Act
        val result = resolver.resolve(MINT)

        // Assert
        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Solana.ScaledUiAmountMultiplierMismatch)
    }

    @Test
    fun `GIVEN mint has no extension WHEN resolve THEN null is returned`() = runTest {
        val resolver = createResolver(provider(null), provider(null))

        val result = resolver.resolve(MINT)

        assertThat((result as Result.Success).data).isNull()
    }

    @Test
    fun `GIVEN first provider fails WHEN resolve THEN the next one is polled instead`() = runTest {
        // Arrange
        val failing = failingProvider()
        val resolver = createResolver(failing, provider(BigDecimal("2")), provider(BigDecimal("2")))

        // Act
        val result = resolver.resolve(MINT)

        // Assert
        assertThat((result as Result.Success).data).isEqualTo(BigDecimal("2"))
    }

    @Test
    fun `GIVEN only one provider answers with a value WHEN resolve THEN transaction is stopped`() = runTest {
        // Arrange
        // A multiplier nobody else confirmed still scales what the card signs
        val resolver = createResolver(provider(BigDecimal("1.5")), failingProvider())

        // Act
        val result = resolver.resolve(MINT)

        // Assert
        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Solana.ScaledUiAmountMultiplierNotCorroborated)
    }

    @Test
    fun `GIVEN only one provider answers with a neutral multiplier WHEN resolve THEN transaction is stopped`() =
        runTest {
            // Arrange
            // Even a plain 1 is a claim about scaling, so an unconfirmed one is refused as well
            val resolver = createResolver(provider(BigDecimal.ONE), failingProvider())

            // Act
            val result = resolver.resolve(MINT)

            // Assert
            assertThat((result as Result.Failure).error)
                .isEqualTo(BlockchainSdkError.Solana.ScaledUiAmountMultiplierNotCorroborated)
        }

    @Test
    fun `GIVEN only one provider answers with no extension WHEN resolve THEN null is returned`() = runTest {
        // Arrange
        // The mint declares no scaling, so the amount reaches the transfer untouched, as for a plain SPL token
        val resolver = createResolver(provider(null), failingProvider())

        // Act
        val result = resolver.resolve(MINT)

        // Assert
        assertThat((result as Result.Success).data).isNull()
    }

    @Test
    fun `GIVEN no provider answers WHEN resolve THEN the network error is rethrown`() = runTest {
        // Arrange
        // Failing to read the multiplier must not be mistaken for the mint declaring no scaling
        val resolver = createResolver(failingProvider(), failingProvider())

        // Act
        val result = resolver.resolve(MINT)

        // Assert
        assertThat((result as Result.Failure).error).isInstanceOf(BlockchainSdkError.Solana.Api::class.java)
    }

    @Test
    fun `GIVEN two providers share a host WHEN resolve THEN only one of them counts`() = runTest {
        // Arrange
        // The same node behind two entries cannot corroborate itself, so its value stays unconfirmed
        val host = "https://same-node.test/rpc?apiKey=first"
        val resolver = createResolver(
            provider(BigDecimal("0.001"), host = host),
            provider(BigDecimal("0.001"), host = "https://same-node.test/rpc?apiKey=second"),
        )

        // Act
        val result = resolver.resolve(MINT)

        // Assert
        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Solana.ScaledUiAmountMultiplierNotCorroborated)
    }

    @Test
    fun `GIVEN two providers agree WHEN resolve THEN the rest are not polled`() = runTest {
        // Arrange
        val untouched = provider(BigDecimal("1.5"))
        val resolver = createResolver(provider(BigDecimal("1.5")), provider(BigDecimal("1.5")), untouched)

        // Act
        resolver.resolve(MINT)

        // Assert
        coVerify(exactly = 0) { untouched.getScaledUiAmountMultiplier(any()) }
    }

    private fun createResolver(vararg providers: SolanaNetworkService) = SolanaScaledUiAmountMultiplierResolver(
        multiNetworkProvider = MultiNetworkProvider(
            providers = providers.toList(),
            blockchain = Blockchain.Solana,
        ),
    )

    private fun provider(multiplier: BigDecimal?, host: String = nextHost()): SolanaNetworkService = mockk {
        every { baseUrl } returns host
        coEvery { getScaledUiAmountMultiplier(any()) } returns Result.Success(multiplier)
    }

    private fun failingProvider(host: String = nextHost()): SolanaNetworkService = mockk {
        every { baseUrl } returns host
        coEvery { getScaledUiAmountMultiplier(any()) } returns
            Result.Failure(BlockchainSdkError.Solana.Api(IllegalStateException("unreachable")))
    }

    private fun nextHost(): String = "https://provider-${hostCounter++}.test/rpc"

    private companion object {
        const val MINT = "2b1kV6DkPAnxd5ixfnxCpjxmKwqjjaYmCZfHsFu24GXo"
    }
}