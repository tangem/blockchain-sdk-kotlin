package com.tangem.blockchain.common.logging

/**
 * External logger
 *
[REDACTED_AUTHOR]
 */
interface BlockchainSDKLogger {

    fun log(level: Level, message: String)

    enum class Level {
        NETWORK,
        TRANSACTION,
    }
}