package com.tangem.blockchain.common.logging

/**
 * External logger
 *
[REDACTED_AUTHOR]
 */
interface BlockchainSDKLogger {

    /**
     * @param level     severity of the entry
     * @param tag       source of the entry — the calling class, or a subsystem such as `NETWORK`
     * @param message   text of the entry
     * @param throwable cause, when the entry reports a failure
     */
    fun log(level: Level, tag: String, message: String, throwable: Throwable? = null)

    /**
     * Deliberately without a `WARNING`: the host app keeps [INFO] and [ERROR] in the exported logs
     * only, so a warning would be dropped from them — log what is worth keeping as [ERROR].
     */
    enum class Level {
        DEBUG,
        INFO,
        ERROR,
    }
}