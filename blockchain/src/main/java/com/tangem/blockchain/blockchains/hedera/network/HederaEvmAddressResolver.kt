package com.tangem.blockchain.blockchains.hedera.network

import com.tangem.Log
import com.tangem.blockchain.blockchains.hedera.HederaUtils
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Resolves the EVM address of a Hedera account by querying every available mirror node at once.
 *
 * Security rules ([REDACTED_TASK_KEY]):
 * - all mirror nodes are queried in parallel;
 * - if more than one node answered, every answer must match, otherwise resolution fails;
 * - if only one node answered, its answer must match the long-zero form of the account ID.
 *
 * A malformed answer counts as no answer: it can only reduce the number of valid answers,
 * which makes the rules stricter, never weaker.
 */
internal class HederaEvmAddressResolver(providers: List<HederaNetworkProvider>) {

    /**
     * Providers are deduplicated by [com.tangem.blockchain.common.NetworkProvider.baseUrl] so that a
     * misconfigured remote config listing the same mirror node twice cannot satisfy the quorum rules
     * with answers from a single, potentially compromised, source. The trailing slash is ignored
     * because remote config may spell the same URL either way.
     *
     * This is a guard against misconfiguration, not a proof of source independence: two different
     * hostnames belonging to the same operator, or two names behind one CDN, are indistinguishable here.
     */
    private val providers = providers.distinctBy { it.baseUrl.trimEnd('/') }

    suspend fun resolveAccountEvmAddress(accountId: String): Result<String> {
        val results = try {
            coroutineScope {
                providers.map { provider -> async { provider.getAccountDetail(accountId) } }.awaitAll()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.Failure(e.toBlockchainSdkError())
        }

        val answers = results
            .mapNotNull { (it as? Result.Success)?.data?.evmAddress }
            .filter(HederaUtils::isValidEvmAddress)
            .map(::normalize)
        val distinct = answers.distinct()

        return when {
            distinct.isEmpty() -> firstFailureOrUnavailable(results)
            distinct.size > 1 -> mismatch(distinct)
            answers.size >= MIN_CONFIRMATIONS -> Result.Success(canonical(distinct.first()))
            else -> confirmAgainstLongZero(accountId, distinct.first())
        }
    }

    private fun firstFailureOrUnavailable(results: List<Result<HederaAccountDetailResponse>>): Result<String> {
        return results.filterIsInstance<Result.Failure>().firstOrNull()
            ?: Result.Failure(BlockchainSdkError.Hedera.EvmAddressUnavailable)
    }

    private fun mismatch(distinct: List<String>): Result<String> {
        Log.error { "Hedera mirror nodes returned ${distinct.size} different EVM addresses: $distinct" }
        return Result.Failure(BlockchainSdkError.Hedera.EvmAddressMismatchBetweenNodes)
    }

    private fun confirmAgainstLongZero(accountId: String, answer: String): Result<String> {
        val expected = HederaUtils.accountIdToEvmAddressOrNull(accountId)?.let(::normalize)
        return if (expected != null && expected == answer) {
            Result.Success(canonical(answer))
        } else {
            Result.Failure(BlockchainSdkError.Hedera.EvmAddressNotConfirmed)
        }
    }

    private fun normalize(evmAddress: String): String {
        return evmAddress.removePrefix("0x").removePrefix("0X").lowercase()
    }

    private fun canonical(normalizedAddress: String): String = "0x$normalizedAddress"

    private companion object {
        const val MIN_CONFIRMATIONS = 2
    }
}