package com.tangem.blockchain.blockchains.hedera

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.hedera.network.HederaAccountDetailResponse
import com.tangem.blockchain.blockchains.hedera.network.HederaEvmAddressResolver
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkProvider
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class HederaEvmAddressResolverTest {

    @Test
    fun resolve_returnsAddress_whenTwoNodesAgreeOnAlias() = runTest {
        val resolver = HederaEvmAddressResolver(
            listOf(providerReturning(ALIAS), providerReturning(ALIAS)),
        )

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(ALIAS)
    }

    @Test
    fun resolve_fails_whenTwoNodesDisagree() = runTest {
        val resolver = HederaEvmAddressResolver(
            listOf(providerReturning(ALIAS), providerReturning(OTHER_ALIAS)),
        )

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Hedera.EvmAddressMismatchBetweenNodes)
    }

    @Test
    fun resolve_returnsAddress_whenSingleAnswerMatchesLongZero() = runTest {
        val resolver = HederaEvmAddressResolver(listOf(providerReturning(LONG_ZERO)))

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat((result as Result.Success).data).isEqualTo(LONG_ZERO)
    }

    @Test
    fun resolve_fails_whenSingleAnswerIsAliasNotMatchingLongZero() = runTest {
        val resolver = HederaEvmAddressResolver(listOf(providerReturning(ALIAS)))

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Hedera.EvmAddressNotConfirmed)
    }

    @Test
    fun resolve_appliesSingleAnswerRule_whenOtherNodeFailed() = runTest {
        val resolver = HederaEvmAddressResolver(
            listOf(providerReturning(LONG_ZERO), providerFailing()),
        )

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat((result as Result.Success).data).isEqualTo(LONG_ZERO)
    }

    @Test
    fun resolve_returnsOriginalNetworkError_whenAllNodesFailed() = runTest {
        val resolver = HederaEvmAddressResolver(listOf(providerFailing(), providerFailing()))

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat((result as Result.Failure).error).isEqualTo(NETWORK_ERROR)
    }

    @Test
    fun resolve_ignoresCaseDifferencesBetweenNodes() = runTest {
        val resolver = HederaEvmAddressResolver(
            listOf(providerReturning(ALIAS.uppercase()), providerReturning(ALIAS)),
        )

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat((result as Result.Success).data).isEqualTo(ALIAS)
    }

    @Test
    fun resolve_treatsMalformedAnswerAsNoAnswer() = runTest {
        val resolver = HederaEvmAddressResolver(
            listOf(providerReturning("0x123"), providerReturning(LONG_ZERO)),
        )

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat((result as Result.Success).data).isEqualTo(LONG_ZERO)
    }

    @Test
    fun resolve_fails_whenEveryAnswerIsMalformed() = runTest {
        val resolver = HederaEvmAddressResolver(
            listOf(providerReturning("0x123"), providerReturning("")),
        )

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Hedera.EvmAddressUnavailable)
    }

    @Test
    fun resolve_fails_whenLongZeroIsNotComputableForAccountId() = runTest {
        val resolver = HederaEvmAddressResolver(listOf(providerReturning(LONG_ZERO)))

        val result = resolver.resolveAccountEvmAddress("1.2.3")

        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Hedera.EvmAddressNotConfirmed)
    }

    @Test
    fun resolve_requiresUnanimity_whenTwoOfThreeNodesAgree() = runTest {
        val resolver = HederaEvmAddressResolver(
            listOf(providerReturning(ALIAS), providerReturning(ALIAS), providerReturning(OTHER_ALIAS)),
        )

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Hedera.EvmAddressMismatchBetweenNodes)
    }

    @Test
    fun resolve_queriesEveryProvider() = runTest {
        val first = providerReturning(ALIAS)
        val second = providerReturning(ALIAS)
        val third = providerReturning(ALIAS)

        HederaEvmAddressResolver(listOf(first, second, third)).resolveAccountEvmAddress(ACCOUNT_ID)

        coVerify(exactly = 1) { first.getAccountDetail(ACCOUNT_ID) }
        coVerify(exactly = 1) { second.getAccountDetail(ACCOUNT_ID) }
        coVerify(exactly = 1) { third.getAccountDetail(ACCOUNT_ID) }
    }

    @Test
    fun resolve_fails_whenDuplicateProviderUrlInflatesQuorumWithSingleAliasSource() = runTest {
        // Same baseUrl reported twice (e.g. a remote-config typo or two aliases for one host).
        // After deduplication only one distinct source remains, so the single-answer rule
        // applies and an alias that doesn't match the long-zero address must be rejected -
        // it must NOT be treated as two independent confirmations of an alias.
        val duplicated = providerReturning(evmAddress = ALIAS, baseUrl = DUPLICATE_URL)
        val duplicatedAgain = providerReturning(evmAddress = ALIAS, baseUrl = DUPLICATE_URL)

        val resolver = HederaEvmAddressResolver(listOf(duplicated, duplicatedAgain))

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Hedera.EvmAddressNotConfirmed)
    }

    @Test
    fun resolve_deduplicatesUrlsDifferingOnlyByTrailingSlash() = runTest {
        // Remote config may spell the same mirror URL with or without a trailing slash; both
        // spellings are one source and must not inflate the quorum.
        val withSlash = providerReturning(evmAddress = ALIAS, baseUrl = "$DUPLICATE_URL/")
        val withoutSlash = providerReturning(evmAddress = ALIAS, baseUrl = DUPLICATE_URL)

        val resolver = HederaEvmAddressResolver(listOf(withSlash, withoutSlash))

        val result = resolver.resolveAccountEvmAddress(ACCOUNT_ID)

        assertThat((result as Result.Failure).error)
            .isEqualTo(BlockchainSdkError.Hedera.EvmAddressNotConfirmed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun resolve_propagatesCancellation_insteadOfReturningFailure() = runTest {
        // The real HederaMirrorRestProvider.getAccountDetail wraps its network call in
        // `catch (e: Exception)`, and CancellationException is an Exception, so the provider
        // itself would normally swallow a cancellation and hand back a Result.Failure.
        // Cancellation must still win: this pins down that resolveAccountEvmAddress never
        // hands control back to its caller (with any Result) once its job has been cancelled -
        // it must complete exceptionally with CancellationException instead. If someone swaps
        // the `catch (CancellationException)` / `catch (Exception)` order in the resolver, or a
        // provider starts swallowing cancellation without the coroutine machinery re-throwing
        // it, this test must fail.
        val hangingProvider = mockk<HederaNetworkProvider>()
        every { hangingProvider.baseUrl } returns "https://hanging.example.com"
        coEvery { hangingProvider.getAccountDetail(any()) } coAnswers {
            try {
                awaitCancellation()
            } catch (e: Exception) {
                // Mirrors HederaMirrorRestProvider's `catch (e: Exception)`.
                Result.Failure(BlockchainSdkError.WrappedThrowable(RuntimeException("swallowed: $e")))
            }
        }

        val resolver = HederaEvmAddressResolver(listOf(hangingProvider))
        val returnedNormally = AtomicInteger(0)

        val job = launch {
            resolver.resolveAccountEvmAddress(ACCOUNT_ID)
            // Reached only if resolveAccountEvmAddress returned a Result instead of the
            // cancellation propagating out of it - that would be the bug under test.
            returnedNormally.incrementAndGet()
        }

        runCurrent()
        job.cancel()
        job.join()

        assertThat(returnedNormally.get()).isEqualTo(0)
        // Guards against a vacuous pass: proves the coroutine actually entered the resolver and
        // reached the suspension point, rather than never having started.
        coVerify(exactly = 1) { hangingProvider.getAccountDetail(ACCOUNT_ID) }
    }

    private fun providerReturning(evmAddress: String, baseUrl: String = uniqueBaseUrl()): HederaNetworkProvider {
        val provider = mockk<HederaNetworkProvider>()
        every { provider.baseUrl } returns baseUrl
        coEvery { provider.getAccountDetail(any()) } returns Result.Success(
            HederaAccountDetailResponse(account = ACCOUNT_ID, evmAddress = evmAddress),
        )
        return provider
    }

    private fun providerFailing(baseUrl: String = uniqueBaseUrl()): HederaNetworkProvider {
        val provider = mockk<HederaNetworkProvider>()
        every { provider.baseUrl } returns baseUrl
        coEvery { provider.getAccountDetail(any()) } returns Result.Failure(NETWORK_ERROR)
        return provider
    }

    private fun uniqueBaseUrl(): String = "https://mirror-${baseUrlCounter.incrementAndGet()}.example.com"

    private companion object {
        const val ACCOUNT_ID = "0.0.1001"
        val LONG_ZERO = "0x" + "0".repeat(37) + "3e9"
        const val ALIAS = "0x1234567890abcdef1234567890abcdef12345678"
        const val OTHER_ALIAS = "0xfedcba9876543210fedcba9876543210fedcba98"
        const val DUPLICATE_URL = "https://mirror-duplicate.example.com"
        val NETWORK_ERROR = BlockchainSdkError.WrappedThrowable(IOException("boom"))
        val baseUrlCounter = AtomicInteger(0)
    }
}