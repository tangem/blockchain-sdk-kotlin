package com.tangem.blockchain.collectibles.providers.solana.model

/**
 * Parsed on-chain state of the program's singleton `Config` account.
 *
 * Everything here is owned by the gacha admin and changeable at runtime through `update_config`, so none of it may be
 * copied into the SDK as a constant — see [GachaConfigParser].
 *
 * @property minFeeBps floor of Tangem's fee corridor in basis points.
 * @property isPaused on-chain `paused`; the two boolean fields are renamed to satisfy the project's naming rule.
 * @property isBuybackDelegationAllowed on-chain `allow_buyback_delegation`.
 * @property bump the bump seed of the `Config` PDA itself.
 */
internal data class GachaConfig(
    val admin: String,
    val usdcMint: String,
    val gachaWallet: String,
    val gachaUsdcAccount: String,
    val feeUsdcAccount: String,
    val minFeeBps: Int,
    val isPaused: Boolean,
    val isBuybackDelegationAllowed: Boolean,
    val bump: Int,
)