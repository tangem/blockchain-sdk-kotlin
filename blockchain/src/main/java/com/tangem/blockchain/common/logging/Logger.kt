package com.tangem.blockchain.common.logging

import com.tangem.blockchain.common.logging.BlockchainSDKLogger.Level
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Internal logger
 *
 * Mirrors `android.util.Log` on purpose, so that SDK code logs through the host app's logger — which
 * reaches both the console and the log file a user attaches to a support request — instead of writing
 * to Logcat directly, where nothing survives the session.
 *
[REDACTED_AUTHOR]
 */
internal object Logger {

    private const val NETWORK_TAG = "NETWORK"
    private const val TRANSACTION_TAG = "TRANSACTION"

    private val loggersFlow = MutableStateFlow(value = setOf<BlockchainSDKLogger>())

    fun d(tag: String, message: String) = log(level = Level.DEBUG, tag = tag, message = message)

    fun i(tag: String, message: String) = log(level = Level.INFO, tag = tag, message = message)

    fun e(tag: String, message: String, throwable: Throwable? = null) =
        log(level = Level.ERROR, tag = tag, message = message, throwable = throwable)

    /** Network traffic, tagged by subsystem rather than by the calling class. */
    fun logNetwork(message: String) = i(tag = NETWORK_TAG, message = message)

    /** Transaction lifecycle, tagged by subsystem rather than by the calling class. */
    fun logTransaction(message: String) = i(tag = TRANSACTION_TAG, message = message)

    fun addLoggers(loggers: List<BlockchainSDKLogger>) {
        loggersFlow.value += loggers
    }

    private fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        val loggers = loggersFlow.value
        if (loggers.isEmpty()) return

        loggers.forEach { it.log(level = level, tag = tag, message = message, throwable = throwable) }
    }
}