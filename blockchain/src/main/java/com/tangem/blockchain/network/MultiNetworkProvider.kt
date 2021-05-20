package com.tangem.blockchain.network

import com.tangem.blockchain.common.CycleListIterator
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.isNetworkError

abstract class MultiNetworkProvider<E>(protected val providers: List<E>) {

    private val providerIterator = CycleListIterator(providers)
    protected var currentProvider = providerIterator.next()

    var retryCounter = 0

    protected fun Result<*>.needsRetry(): Boolean {
        retryCounter += 1
        return if (this.isNetworkError() && retryCounter < providers.size) {
            currentProvider = providerIterator.next()
            true
        } else {
            retryCounter = 0
            false
        }
    }

    protected fun SimpleResult.needsRetry(): Boolean {
        retryCounter += 1
        return if (this.isNetworkError() && retryCounter < providers.size) {
            currentProvider = providerIterator.next()
            true
        } else {
            retryCounter = 0
            false
        }
    }
}