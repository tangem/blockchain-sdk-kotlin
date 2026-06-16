package com.tangem.blockchain.common

/**
 * Blockchain feature toggles
 *
[REDACTED_AUTHOR]
 */
class BlockchainFeatureToggles(
    val isYieldSupplyEnabled: Boolean,
    val isYieldModeSwapEnabled: Boolean = false,
    val isPendingTransactionsEnabled: Boolean = false,
    val isSolanaTxHistoryEnabled: Boolean = false,
    val isSolanaScaledUiAmountEnabled: Boolean = false,
    val isHederaErc20Enabled: Boolean = false,
    val isStateOverrideGasEstimateEnabled: Boolean = false,
)