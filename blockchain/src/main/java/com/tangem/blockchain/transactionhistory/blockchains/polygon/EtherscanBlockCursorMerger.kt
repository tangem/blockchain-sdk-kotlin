package com.tangem.blockchain.transactionhistory.blockchains.polygon

/** A raw record of one Etherscan list with the block it is in; [item] is `null` when it is not a history item */
internal data class BlockEntry<T>(val block: Long, val item: T)

/** One list's answer for `endblock = E`, newest first; [isFull] when it returned the whole page it was asked for */
internal data class BlockStream<T>(val items: List<BlockEntry<T>>, val isFull: Boolean)

/**
 * [nextEndBlock] is the `endblock` of the next page, `null` when the history is over. [truncatedBlock] is set when a
 * full list returned nothing but that block, so [items] may miss its records past the page and it must be fetched whole
 */
internal data class MergedPage<T>(
    val items: List<BlockEntry<T>>,
    val nextEndBlock: Long?,
    val truncatedBlock: Long? = null,
)

/**
 * Merges `txlist` and `txlistinternal`, which page independently, into pages that never split a block: a transaction
 * is in one block, so its transfer and its refund always land on the same page. `endblock` is inclusive, so the
 * boundary block of a full list is fetched again whole on the next page.
 */
internal object EtherscanBlockCursorMerger {

    fun <T> merge(streams: List<BlockStream<T>>): MergedPage<T> {
        val entries = streams.flatMap(BlockStream<T>::items)
        val boundary = streams
            .filter { it.isFull && it.items.isNotEmpty() }
            .maxOfOrNull { it.items.last().block }
            ?: return MergedPage(items = entries.sortedByDescending(BlockEntry<T>::block), nextEndBlock = null)

        val above = entries.filter { it.block > boundary }
        if (above.isEmpty()) {
            return MergedPage(
                items = entries.filter { it.block == boundary },
                nextEndBlock = boundary - 1,
                truncatedBlock = boundary,
            )
        }
        return MergedPage(items = above.sortedByDescending(BlockEntry<T>::block), nextEndBlock = boundary)
    }
}