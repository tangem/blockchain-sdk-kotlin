package com.tangem.blockchain.collectibles.models

/**
 * Parsed on-chain state of a user's collectibles vault.
 *
 * @property dayIndex UTC day the [spentToday] bucket belongs to, as `floor(unix_seconds / 86400)`. The bucket rolls
 * over lazily — only inside `open_pack` — so a stale [dayIndex] means the full [dailyCap] is available again.
 */
data class CollectiblesVault(
    val address: String,
    val coldOwner: String,
    val hotDelegate: String,
    val perSpinCap: Long,
    val dailyCap: Long,
    val spentToday: Long,
    val dayIndex: Long,
    val liveBuybackMint: String?,
    val liveBuybackToken: String?,
)