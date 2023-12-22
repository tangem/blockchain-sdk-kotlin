package com.tangem.blockchain.common

@Suppress("IteratorNotThrowingNoSuchElementException")
internal class CycleListIterator<E>(private val elements: List<E>) : Iterator<E> {

    private var currentIndex = -1

    override fun next(): E {
        currentIndex = getNextIndex()
        return elements[currentIndex]
    }

    fun peekNext(): E {
        return elements[getNextIndex()]
    }

    private fun getNextIndex(): Int {
        return if (currentIndex < elements.lastIndex) {
            currentIndex + 1
        } else {
            0
        }
    }

    override fun hasNext() = true
}
