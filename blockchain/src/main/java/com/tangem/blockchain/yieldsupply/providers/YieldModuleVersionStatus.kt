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

    /**
     * Module version cannot be determined (e.g. transient RPC failure). Callers MUST NOT proceed
     * with a swap, since the module may be deployed-but-outdated and an unwrapped call would revert.
     * @property reason short technical explanation, suitable for logs
     */
    data class Indeterminate(val reason: String) : YieldModuleVersionStatus()
}

/**
 * Thrown when a yield module upgrade is required but unavailable.
 * @property currentImplementation the current implementation address of the outdated module
 */
class YieldModuleUpgradeUnavailableException(
    val currentImplementation: String,
) : IllegalStateException(
    "Yield module is outdated (implementation: $currentImplementation) and cannot be upgraded",
)

/**
 * Thrown when the yield module version cannot be determined (RPC error, etc.) and proceeding
 * with the wrapped/unwrapped call would risk routing through an outdated proxy.
 * @property reason short technical explanation
 */
class YieldModuleVersionIndeterminateException(
    val reason: String,
) : IllegalStateException("Yield module version cannot be determined: $reason")