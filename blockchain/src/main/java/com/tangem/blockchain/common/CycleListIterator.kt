package com.tangem.blockchain.common

internal class CycleListIterator<E>(private val elements: List<E>) : Iterator<E> {

    private var currentIndex = 0

    override fun next() : E {
        currentIndex = getNextIndex()
        return elements[currentIndex]
    }

    fun peekNext(): E {
        return elements[getNextIndex()]
    }

    private fun getNextIndex(): Int {
        return if (currentIndex < elements.lastIndex) {
            currentIndex ++
        } else {
            0
        }
    }

    override fun hasNext() = true
}