package com.tangem.blockchain.nft.providers.moralis.solana

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class MoralisCursorPaginatorTest {

    @Test
    fun `reads pages until the cursor ends regardless of page size`() = runTest {
        val pages = mapOf(null to Page(listOf(1), "a"), "a" to Page(emptyList(), "b"), "b" to Page(listOf(2, 3), null))
        val requested = mutableListOf<String?>()

        val items = MoralisCursorPaginator().loadAll { cursor ->
            requested += cursor
            pages.getValue(cursor)
        }

        assertThat(items).containsExactly(1, 2, 3).inOrder()
        assertThat(requested).containsExactly(null, "a", "b").inOrder()
    }

    @Test
    fun `treats an empty cursor as the last page`() = runTest {
        var calls = 0

        val items = MoralisCursorPaginator().loadAll { cursor ->
            calls++
            Page(listOf(1), "")
        }

        assertThat(items).containsExactly(1)
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `stops at the page limit`() = runTest {
        var calls = 0

        val items = MoralisCursorPaginator(maxPages = 3).loadAll { cursor ->
            calls++
            Page(listOf(calls), "next-$calls")
        }

        assertThat(items).containsExactly(1, 2, 3).inOrder()
        assertThat(calls).isEqualTo(3)
    }

    @Test
    fun `GIVEN a later page fails once WHEN load THEN it is requested again with the same cursor`() = runTest {
        // Arrange
        val requested = mutableListOf<String?>()

        // Act
        val items = MoralisCursorPaginator().loadAll { cursor ->
            requested += cursor
            when {
                cursor == null -> Page(listOf(1), "a")
                requested.count { it == "a" } == 1 -> error("timeout")
                else -> Page(listOf(2), null)
            }
        }

        // Assert
        assertThat(items).containsExactly(1, 2).inOrder()
        assertThat(requested).containsExactly(null, "a", "a").inOrder()
    }

    @Test
    fun `GIVEN a later page keeps failing WHEN load THEN loaded items are returned after one retry`() = runTest {
        // Arrange
        val requested = mutableListOf<String?>()

        // Act
        val items = MoralisCursorPaginator().loadAll { cursor ->
            requested += cursor
            if (cursor == null) Page(listOf(1), "a") else error("timeout")
        }

        // Assert
        assertThat(items).containsExactly(1)
        assertThat(requested).containsExactly(null, "a", "a").inOrder()
    }

    @Test
    fun `first page failing twice gives nothing`() = runTest {
        var calls = 0

        val items = MoralisCursorPaginator().loadAll<Int> {
            calls++
            error("timeout")
        }

        assertThat(items).isEmpty()
        assertThat(calls).isEqualTo(2)
    }
}