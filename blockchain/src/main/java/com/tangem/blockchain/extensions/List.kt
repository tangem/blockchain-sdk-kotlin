package com.tangem.blockchain.extensions

/**
 * Return only `A` elements not contained in `B` list
 */
fun <A, B> List<A>.filterWith(list: List<B>, predicate: (A, B) -> Boolean): MutableList<A> {
    val resultList = mutableListOf<A>()
    this.forEach a@{ a ->
        list.forEach b@{ b ->
            if (!predicate(a, b)) return@a
        }
        resultList.add(a)
    }
    return resultList
}
