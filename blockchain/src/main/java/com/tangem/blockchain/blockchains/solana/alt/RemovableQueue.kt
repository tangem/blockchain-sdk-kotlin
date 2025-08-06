package com.tangem.blockchain.blockchains.solana.alt

class RemovableQueue<T>(items: List<T>) {
    private val queue = items.toMutableList()

    fun takeFirst(): T {
        return if (queue.isNotEmpty()) queue.removeAt(0) else error("Queue is empty")
    }

    fun isEmpty(): Boolean = queue.isEmpty()
    fun size(): Int = queue.size
}