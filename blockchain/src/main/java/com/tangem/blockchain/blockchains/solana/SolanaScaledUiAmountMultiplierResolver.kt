package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.net.URI
import java.net.URISyntaxException

/**
 * Resolves the mint's `scaledUiAmountConfig` multiplier by asking independent providers and returning the value
 * only if they agree.
 *
 * The multiplier scales the amount the card signs, and a single provider can keep any single response internally
 * consistent, so agreement between independent sources is what makes the value trustworthy. A disagreement is
 * treated as hostile and stops the transaction - an omitted extension counts as an answer too, so dropping it
 * from the response does not pass either.
 *
 * | Answers collected                                     | Result                                          |
 * |-------------------------------------------------------|-------------------------------------------------|
 * | Two providers, same value                             | accepted                                        |
 * | Two providers, different values                       | `ScaledUiAmountMultiplierMismatch`              |
 * | Only one provider reachable, value is null            | accepted, the mint declares no scaling          |
 * | Only one provider reachable, any real multiplier      | `ScaledUiAmountMultiplierNotCorroborated`       |
 * | Nobody answered                                       | the underlying network error is rethrown        |
 */
internal class SolanaScaledUiAmountMultiplierResolver(
    private val multiNetworkProvider: MultiNetworkProvider<SolanaNetworkService>,
) {

    suspend fun resolve(mintAddress: String): Result<BigDecimal?> = poll(mintAddress).toResult()

    /** Polls providers one batch at a time until [REQUIRED_ANSWERS] of them answer or the list runs out. */
    private suspend fun poll(mintAddress: String): Poll = coroutineScope {
        val answers = mutableListOf<BigDecimal?>()
        var lastFailure: Result.Failure? = null
        val providers = distinctProviders().iterator()

        while (answers.size < REQUIRED_ANSWERS && providers.hasNext()) {
            val batch = buildList {
                while (size < REQUIRED_ANSWERS - answers.size && providers.hasNext()) {
                    add(providers.next())
                }
            }

            batch
                .map { provider -> async { provider.getScaledUiAmountMultiplier(mintAddress) } }
                .awaitAll()
                .forEach { result ->
                    when (result) {
                        is Result.Success -> answers.add(result.data)
                        is Result.Failure -> lastFailure = result
                    }
                }
        }

        Poll(answers = answers, lastFailure = lastFailure)
    }

    /**
     * Providers backed by the same host cannot corroborate each other, so only the first one of each host is asked.
     * The list comes from the backend and may well repeat a host under different keys.
     */
    private fun distinctProviders(): List<SolanaNetworkService> =
        multiNetworkProvider.providers.distinctBy { it.host() }

    private fun SolanaNetworkService.host(): String = try {
        URI(baseUrl).host ?: baseUrl
    } catch (e: URISyntaxException) {
        Logger.logTransaction("Failed to read the host of a Solana provider: ${e.message}")
        baseUrl
    }

    private fun Poll.toResult(): Result<BigDecimal?> {
        val first = answers.firstOrNull()

        return when {
            answers.size >= REQUIRED_ANSWERS -> {
                if (answers.all { it.matches(first) }) {
                    Result.Success(first)
                } else {
                    reject("providers disagree", BlockchainSdkError.Solana.ScaledUiAmountMultiplierMismatch)
                }
            }
            // The only reachable provider declares no scaling, so the amount reaches the transfer untouched,
            // exactly as for a plain SPL token.
            answers.size == 1 && first == null -> Result.Success(null)
            // A value nobody else confirmed still scales what the card signs, so it is not trusted.
            answers.size == 1 -> reject(
                reason = "a single provider answered",
                error = BlockchainSdkError.Solana.ScaledUiAmountMultiplierNotCorroborated,
            )
            // Failing to read the multiplier is not the same as the mint declaring no scaling: surface the error
            // instead of silently signing an unscaled amount.
            else -> reject(
                reason = "no provider answered",
                error = lastFailure?.error ?: BlockchainSdkError.Solana.ScaledUiAmountMultiplierNotCorroborated,
            )
        }
    }

    /** Refusing to send is invisible in the app beyond an error code, so the reason is worth a log line. */
    private fun reject(reason: String, error: BlockchainError): Result.Failure {
        Logger.logTransaction("Scaled UI amount multiplier rejected: $reason")

        return Result.Failure(error)
    }

    private data class Poll(val answers: List<BigDecimal?>, val lastFailure: Result.Failure?)

    /** Compares by value, so that 0.5 and 0.50 reported by different providers count as the same answer. */
    private fun BigDecimal?.matches(other: BigDecimal?): Boolean = when {
        this == null || other == null -> this == null && other == null
        else -> compareTo(other) == 0
    }

    private companion object {

        /** How many independent providers have to report the same multiplier. */
        const val REQUIRED_ANSWERS = 2
    }
}