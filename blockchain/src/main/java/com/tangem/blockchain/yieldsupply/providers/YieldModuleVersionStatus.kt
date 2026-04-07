package com.tangem.blockchain.yieldsupply.providers

/**
 * Represents the version status of a user's Yield Module contract.
 */
sealed class YieldModuleVersionStatus {

    /** Module is up-to-date, no upgrade needed */
    data object UpToDate : YieldModuleVersionStatus()

    /**
     * Module is outdated and can be upgraded.
     * @property currentImplementation the current implementation address of the module
     * @property latestImplementation the latest implementation address to upgrade to
     */
    data class UpgradeAvailable(
        val currentImplementation: String,
        val latestImplementation: String,
    ) : YieldModuleVersionStatus()

    /**
     * Module is outdated but cannot be upgraded (factory implementation differs from known latest).
     * @property currentImplementation the current implementation address of the module
     */
    data class UpgradeUnavailable(
        val currentImplementation: String,
    ) : YieldModuleVersionStatus()

    /** Module is not deployed */
    data object NotDeployed : YieldModuleVersionStatus()
}