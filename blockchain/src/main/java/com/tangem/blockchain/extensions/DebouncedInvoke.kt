package com.tangem.blockchain.extensions

/**
 * Class wrapping method invocation to be debounced within specified interval [invokeIntervalMillis].
 *
 * @param invokeIntervalMillis interval [invokeIntervalMillis]
 */
internal class DebouncedInvoke(
    private val invokeIntervalMillis: Long = 10000L,
) {
    private var lastInvokeTime = 0L

    /** Call executable [block] with specified interval or force execution with [forceUpdate] */
    suspend fun invokeOnExpire(forceUpdate: Boolean = false, block: suspend () -> Unit) {
        if (forceUpdate) {
            block()
        } else if (isExpired()) {
            lastInvokeTime = System.currentTimeMillis()
            block()
        }
    }

    private fun isExpired(): Boolean {
        return System.currentTimeMillis() - lastInvokeTime >= invokeIntervalMillis
    }
}