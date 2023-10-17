package com.tangem.blockchain.extensions

import android.os.SystemClock

/**
 * Class wrapping method invocation to be debounced within specified interval [invokeIntervalMillis].
 *
 * @param invokeIntervalMillis interval [invokeIntervalMillis]
 */
internal class DebouncedInvoke(
    private val invokeIntervalMillis: Long = 10000L,
) {
    private var lastInvokeTime = 0L
    private var maybeException: Throwable? = null

    /** Call executable [block] with specified interval or force execution with [forceUpdate] */
    suspend fun invokeOnExpire(forceUpdate: Boolean = false, block: suspend () -> Unit) {
        if (forceUpdate) {
            callWithException(block)
        } else if (isExpired()) {
            maybeException = null
            lastInvokeTime = SystemClock.elapsedRealtime()
            callWithException(block)
        } else if (maybeException != null) {
            throw maybeException as Throwable
        }
    }

    private suspend fun callWithException(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            maybeException = e
            throw e
        }
    }

    private fun isExpired(): Boolean {
        return SystemClock.elapsedRealtime() - lastInvokeTime >= invokeIntervalMillis
    }
}