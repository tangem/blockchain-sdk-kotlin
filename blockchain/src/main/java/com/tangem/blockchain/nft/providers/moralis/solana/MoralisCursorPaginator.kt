package com.tangem.blockchain.nft.providers.moralis.solana

import com.tangem.blockchain.common.logging.Logger
import kotlinx.coroutines.CancellationException

internal data class Page<T>(val items: List<T>, val nextCursor: String?)

/**
 * Reads a cursor-paginated endpoint to the end. A page may be shorter than the limit while the cursor is still
 * alive, so only the cursor ends the read. A failed page is requested once more with the same cursor; if it fails
 * again the read ends, keeping what was loaded. Retrying without a bound would never end on a wallet the API can not
 * serve, so both the retries and the page count are capped.
 */
internal class MoralisCursorPaginator(private val maxPages: Int = MAX_PAGES) {

    suspend fun <T> loadAll(fetchPage: suspend (cursor: String?) -> Page<T>): List<T> {
        val items = mutableListOf<T>()
        var cursor: String? = null

        repeat(maxPages) { pageIndex ->
            val page = fetchWithRetry(cursor, fetchPage) ?: run {
                Logger.logNetwork("MoralisCursorPaginator: page $pageIndex failed twice, keeping ${items.size} items")
                return items
            }
            items += page.items
            cursor = page.nextCursor?.takeIf { it.isNotEmpty() } ?: return items
        }

        Logger.logNetwork("MoralisCursorPaginator: stopped at $maxPages pages with ${items.size} items")
        return items
    }

    private suspend fun <T> fetchWithRetry(cursor: String?, fetchPage: suspend (cursor: String?) -> Page<T>): Page<T>? {
        repeat(ATTEMPTS_PER_PAGE) { attempt ->
            try {
                return fetchPage(cursor)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.logNetwork("MoralisCursorPaginator: attempt ${attempt + 1} for cursor $cursor failed: $e")
            }
        }
        return null
    }

    private companion object {
        const val MAX_PAGES = 20
        const val ATTEMPTS_PER_PAGE = 2
    }
}