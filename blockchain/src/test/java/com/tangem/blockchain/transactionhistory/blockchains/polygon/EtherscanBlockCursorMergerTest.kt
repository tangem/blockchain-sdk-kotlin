package com.tangem.blockchain.transactionhistory.blockchains.polygon

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EtherscanBlockCursorMergerTest {

    @Test
    fun `two exhausted streams are merged newest first and close the history`() {
        val page = EtherscanBlockCursorMerger.merge(
            listOf(
                BlockStream(items = listOf(entry(10), entry(7)), isFull = false),
                BlockStream(items = listOf(entry(9)), isFull = false),
            ),
        )

        assertThat(page).isEqualTo(MergedPage(items = listOf(entry(10), entry(9), entry(7)), nextEndBlock = null))
    }

    @Test
    fun `entries of the boundary block are left for the next page`() {
        // The internal stream is full down to block 8: anything at 8 or below may continue on its next page
        val page = EtherscanBlockCursorMerger.merge(
            listOf(
                BlockStream(items = listOf(entry(10), entry(8), entry(5)), isFull = false),
                BlockStream(items = listOf(entry(9), entry(8)), isFull = true),
            ),
        )

        assertThat(page).isEqualTo(MergedPage(items = listOf(entry(10), entry(9)), nextEndBlock = 8))
    }

    @Test
    fun `the higher boundary of two full streams wins`() {
        val page = EtherscanBlockCursorMerger.merge(
            listOf(
                BlockStream(items = listOf(entry(10), entry(6)), isFull = true),
                BlockStream(items = listOf(entry(9), entry(8)), isFull = true),
            ),
        )

        assertThat(page).isEqualTo(MergedPage(items = listOf(entry(10), entry(9)), nextEndBlock = 8))
    }

    @Test
    fun `a page that is all one block is marked truncated and the cursor moves below it`() {
        val page = EtherscanBlockCursorMerger.merge(
            listOf(
                BlockStream(items = listOf(entry(8, "a"), entry(8, "b")), isFull = true),
                BlockStream(items = listOf(entry(8, "c"), entry(3)), isFull = false),
            ),
        )

        assertThat(page).isEqualTo(
            MergedPage(
                items = listOf(entry(8, "a"), entry(8, "b"), entry(8, "c")),
                nextEndBlock = 7,
                truncatedBlock = 8,
            ),
        )
    }

    private fun entry(block: Long, id: String = block.toString()) = BlockEntry(block = block, item = id)
}