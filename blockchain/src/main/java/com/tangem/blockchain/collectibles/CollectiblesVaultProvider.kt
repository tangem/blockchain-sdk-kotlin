package com.tangem.blockchain.collectibles

import com.tangem.blockchain.collectibles.models.CollectiblesVault
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result

/**
 * Access to the `tangem_gacha_vault` program.
 *
 * Building and sending are deliberately separate: the provider returns wire bytes, and the caller sends them with
 * the wallet manager's existing `send(transactionData, signer)`, which strips the message, has the card sign it,
 * splices the signature into its slot and broadcasts to every provider.
 */
interface CollectiblesVaultProvider {

    fun isCollectiblesVaultSupported(): Boolean

    /** `PDA(["vault", cold_owner])`. Pure derivation, no network access. */
    fun getCollectiblesVaultAddress(): Result<String>

    /**
     * Whether this wallet has already been onboarded, i.e. its vault account exists on chain and belongs to it.
     *
     * A missing account is a plain `false`, not an error. Prefer this over [getCollectiblesVault] when only the fact matters —
     * it is the same single request, but it does not force the caller to treat null as a separate case.
     */
    suspend fun isCollectiblesVaultInitialized(): Result<Boolean>

    /** Returns null when the vault account does not exist — that means onboarding has not happened, not an error. */
    suspend fun getCollectiblesVault(): Result<CollectiblesVault?>

    /**
     * @param hotDelegate  base58 key allowed to pay for spins; produced outside this module
     * @param perSpinCap   base units of USDC, must satisfy `0 < perSpinCap <= dailyCap`
     * @param dailyCap     base units of USDC, a UTC-day bucket rather than a rolling window
     */
    suspend fun buildInitCollectiblesVault(
        hotDelegate: String,
        perSpinCap: Long,
        dailyCap: Long,
    ): Result<TransactionData.Compiled>

    /** Fails on-chain while a buyback delegate is live, or if the vault still owns token accounts. */
    suspend fun buildCloseCollectiblesVault(): Result<TransactionData.Compiled>
}