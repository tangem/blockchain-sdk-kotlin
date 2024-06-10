package com.tangem.blockchain.common.logging

import com.tangem.blockchain.common.logging.BlockchainSDKLogger.Level
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Internal logger
 *
[REDACTED_AUTHOR]
 */
internal object Logger {

    private val loggersFlow = MutableStateFlow(value = setOf<BlockchainSDKLogger>())

    fun logNetwork(message: String) {
        logInternal(Level.NETWORK, message)
    }

    fun addLoggers(loggers: List<BlockchainSDKLogger>) {
        loggersFlow.value += loggers
    }

    private fun logInternal(level: Level, message: String) {
        if (loggersFlow.value.isEmpty()) return

        loggersFlow.value.forEach { it.log(level, message) }
    }
}