package com.tangem.blockchain.common

/**
 * Blockchain feature toggles
 *
[REDACTED_AUTHOR]
 */
class BlockchainFeatureToggles(
    val isYieldSupplyEnabled: Boolean,
    val isPendingTransactionsEnabled: Boolean = false,
)