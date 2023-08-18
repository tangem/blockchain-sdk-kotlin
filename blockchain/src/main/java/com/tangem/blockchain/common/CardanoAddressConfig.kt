package com.tangem.blockchain.common

object CardanoAddressConfig {

    /**
     * Indicates whether to use the new 128-byte address instead of the old 32-byte one
     * We should keep old 32 byte addresses for backward compatibility
     */
    var useExtendedAddressing = false

}