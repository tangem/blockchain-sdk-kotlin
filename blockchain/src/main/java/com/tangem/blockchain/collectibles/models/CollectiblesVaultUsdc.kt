package com.tangem.blockchain.collectibles.models

/**
 * The USDC of a collectibles vault: the token account holding it, and what that account's mint is. Both are fixed
 * once the program is deployed — the mint is written by `initialize_config` and no instruction rewrites it, and the
 * address is derived from it.
 *
 * @property address the vault's associated token account, the destination of a top-up
 * @property mint    the USDC mint as the on-chain config names it
 */
data class CollectiblesVaultUsdc(
    val address: String,
    val mint: String,
)