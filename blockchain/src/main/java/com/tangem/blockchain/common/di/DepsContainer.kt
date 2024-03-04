package com.tangem.blockchain.common.di

import com.tangem.blockchain.common.BlockchainSdkConfig
import kotlin.properties.Delegates

/**
 * Container for keeping dependencies
 *
[REDACTED_AUTHOR]
 */
internal object DepsContainer {

    var blockchainSdkConfig: BlockchainSdkConfig by Delegates.notNull()
        private set

    /** Save dependencies that provided on BlockchainSDK's creation */
    fun onInit(config: BlockchainSdkConfig) {
        blockchainSdkConfig = config
    }
}