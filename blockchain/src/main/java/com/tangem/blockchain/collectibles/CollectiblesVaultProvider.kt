package com.tangem.blockchain.collectibles

import com.tangem.blockchain.collectibles.models.CollectiblesVault
import com.tangem.blockchain.collectibles.models.CollectiblesVaultUsdc
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result

/**
 * Access to the `tangem_gacha_vault` program.
 *
 * Building and sending are deliberately separate: the provider returns wire bytes, and the caller sends them with
 * the wallet manager's existing `send(transactionData, signer)`, which strips the message, has the card sign it,
 * splices the signature into its slot and broadcasts to every provider.
 *
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
     * The vault's USDC token account and the mint it holds, the mint being read from the on-chain config rather
     * than held as a constant here. Costs the one request that reads it; both fields are immutable afterwards.
     */
    suspend fun getCollectiblesVaultUsdc(): Result<CollectiblesVaultUsdc>

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

    /**
     * Rotates the hot key and the caps of an existing vault. A null argument leaves the field as it is; all three
     * being null is rejected rather than turned into a card tap that changes nothing.
     *
     * @param hotDelegate  base58 key allowed to pay for spins, or null to keep the current one
     * @param perSpinCap   base units of USDC, must satisfy `0 < perSpinCap <= dailyCap` over the merged state
     * @param dailyCap     base units of USDC, so either cap can be refused because of the other's stored value
     */
    suspend fun buildUpdateCollectiblesVault(
        hotDelegate: String?,
        perSpinCap: Long?,
        dailyCap: Long?,
    ): Result<TransactionData.Compiled>

    /**
     * Closes the vault's USDC account and then the vault itself, in that order and in one transaction.
     *
     * Fails on-chain while a buyback delegate is live (`6014`), or with `6015` if any USDC is left on the vault's
     * token account. Prizes still held by the vault do not block the close: their accounts, and the rent paid for

     */
    suspend fun buildCloseCollectiblesVault(): Result<TransactionData.Compiled>

    /**
     * Pays for one spin from the vault: a top-level SPL Memo `"<memo>:open"`, which is what the gacha counts the
     * spin by, then `purchase_pack` with the same memo. The hot key is the only signer and therefore the fee payer.
     *
     * Not idempotent: the program does not deduplicate memos, so a second call with the same [memo] is charged
     * again. While the outcome of a call is unknown, check the signature status instead of calling again.
     *
     * @param hotDelegate  base58 key the vault's session key (`hot_delegate`) holds
     * @param amount       base units of USDC — the pack price exactly as the gacha expects it; the program only
     *                     checks it is positive, and a wrong one is charged and lost
     * @param feeAmount    Tangem's fee in base units of USDC, charged on top of [amount]
     * @param memo         `memo` from `/api/generatePack`, without `:open` — the suffix is appended here
     * @param signer       signs the message with the hot key, returning a 64-byte ed25519 signature
     *                     (not a `TransactionSigner`: `send` signs into the wallet's own slot, the hot key is not it)
     */
    suspend fun sendPurchasePack(
        hotDelegate: String,
        amount: Long,
        feeAmount: Long,
        memo: String,
        signer: suspend (message: ByteArray) -> Result<ByteArray>,
    ): Result<TransactionSendResult>
}