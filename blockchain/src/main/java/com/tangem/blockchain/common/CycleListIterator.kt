package com.tangem.blockchain.common

class CycleListIterator<E>(val elements: List<E>) : Iterator<E> {

    private var providerIterator = elements.iterator()

    override fun next() : E {
        return if (providerIterator.hasNext()) {
            providerIterator.next()
        } else {
            providerIterator = elements.iterator()
            providerIterator.next()
        }
    }

    override fun hasNext() = true
}