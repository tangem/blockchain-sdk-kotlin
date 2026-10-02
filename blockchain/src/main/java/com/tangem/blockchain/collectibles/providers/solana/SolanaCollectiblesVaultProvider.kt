package com.tangem.blockchain.collectibles.providers.solana

import android.os.SystemClock
import com.tangem.blockchain.blockchains.solana.SolanaTransactionBuilder
import com.tangem.blockchain.blockchains.solana.SolanaNetworkService
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.blockchains.solana.solanaj.core.SolanaTransaction
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaAssociatedTokenProgram
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaMemoProgram
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import com.tangem.blockchain.collectibles.CollectiblesVaultProvider
import com.tangem.blockchain.collectibles.models.CollectiblesVault
import com.tangem.blockchain.collectibles.models.CollectiblesVaultUsdc
import com.tangem.blockchain.collectibles.providers.solana.instruction.GachaCloseTokenAccountInstruction
import com.tangem.blockchain.collectibles.providers.solana.instruction.GachaCloseVaultInstruction
import com.tangem.blockchain.collectibles.providers.solana.instruction.GachaInitVaultInstruction
import com.tangem.blockchain.collectibles.providers.solana.instruction.GachaPurchasePackInstruction
import com.tangem.blockchain.collectibles.providers.solana.instruction.GachaUpdateVaultInstruction
import com.tangem.blockchain.collectibles.providers.solana.model.GachaConfig
import com.tangem.blockchain.collectibles.providers.solana.model.GachaConfigParser
import com.tangem.blockchain.collectibles.providers.solana.model.GachaVaultParser
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import io.ktor.util.decodeBase64Bytes
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction

internal class SolanaCollectiblesVaultProvider(
    private val coldOwner: PublicKey,
    private val multiNetworkProvider: MultiNetworkProvider<SolanaNetworkService>,
) : CollectiblesVaultProvider {

    private val transactionBuilder = SolanaTransactionBuilder(
        account = coldOwner,
        multiNetworkProvider = multiNetworkProvider,
    )

    override fun isCollectiblesVaultSupported(): Boolean = true

    override fun getCollectiblesVaultAddress(): Result<String> =
        when (val result = vaultAddress()) {
            is Result.Success -> Result.Success(result.data.toBase58())
            is Result.Failure -> result
        }

    override suspend fun isCollectiblesVaultInitialized(): Result<Boolean> {
        return when (val result = getCollectiblesVault()) {
            is Result.Success -> Result.Success(result.data != null)
            is Result.Failure -> result
        }
    }

    override suspend fun getCollectiblesVault(): Result<CollectiblesVault?> {
        val vault = vaultAddress().successOr { return it }
        val result = multiNetworkProvider.performRequest { getAccountInfoIfExist(vault) }

        val accountInfo = when (result) {
            is Result.Success -> result.data
            is Result.Failure -> if (result.error is BlockchainSdkError.AccountNotFound) {
                return Result.Success(null)
            } else {
                return result
            }
        }

        if (accountInfo.owner != GachaVaultProgram.programId.toBase58()) {
            return Result.Failure(BlockchainSdkError.CustomError("Vault account is not owned by the gacha program"))
        }

        val rawData = accountInfo.data.firstOrNull()
            ?: return Result.Failure(BlockchainSdkError.CustomError("Vault account has no data"))

        val parsed =
            runCatching { GachaVaultParser.parse(vault, rawData.decodeBase64Bytes()) }.getOrNull()
                ?: return Result.Failure(BlockchainSdkError.CustomError("Failed to parse vault account"))

        // The address was derived from this wallet's key, so a different cold owner means the account is not what
        // it claims to be. Reported rather than silently accepted.
        if (parsed.coldOwner != coldOwner.toBase58()) {
            return Result.Failure(BlockchainSdkError.CustomError("Vault belongs to a different cold owner"))
        }

        return Result.Success(parsed)
    }

    override suspend fun getCollectiblesVaultUsdc(): Result<CollectiblesVaultUsdc> {
        val vault = vaultAddress().successOr { return it }
        val mint = usdcMint().successOr { return it }
        val vaultUsdc = vaultUsdcAddress(vault = vault, mint = mint).successOr { return it }

        return Result.Success(
            CollectiblesVaultUsdc(
                address = vaultUsdc.toBase58(),
                mint = mint.toBase58(),
            ),
        )
    }

    override suspend fun buildInitCollectiblesVault(
        hotDelegate: String,
        perSpinCap: Long,
        dailyCap: Long,
    ): Result<TransactionData.Compiled> {
        validateCaps(perSpinCap = perSpinCap, dailyCap = dailyCap)?.let { return it }

        val vault = vaultAddress().successOr { return it }
        val delegate = publicKey(hotDelegate).successOr { return it }

        val initVault = GachaInitVaultInstruction.create(
            vault = vault,
            coldOwner = coldOwner,
            payer = coldOwner,
            hotDelegate = delegate,
            perSpinCap = perSpinCap,
            dailyCap = dailyCap,
        )

        val createVaultUsdc = createVaultUsdc(vault).successOr { return it }

        // Both instructions allocate an account, hence the create-an-account budget rather than the default one.
        // They are bundled rather than sent one after another so that the vault and the account it receives USDC on
        // either both exist or neither does — and because the card signs the pair exactly once.
        val transaction = transactionBuilder.buildUnsignedTransaction(
            instructions = listOf(initVault, createVaultUsdc),
            computeUnitLimit = SolanaTransactionBuilder.CREATE_NEW_ACCOUNT_CU_LIMIT,
            computeUnitPrice = SolanaTransactionBuilder.CREATE_NEW_ACCOUNT_CU_PRICE,
        ).successOr { return it }

        return Result.Success(transaction.toCompiled())
    }

    override suspend fun buildUpdateCollectiblesVault(
        hotDelegate: String?,
        perSpinCap: Long?,
        dailyCap: Long?,
    ): Result<TransactionData.Compiled> {
        if (hotDelegate == null && perSpinCap == null && dailyCap == null) {
            return Result.Failure(BlockchainSdkError.CustomError("Nothing to update"))
        }

        validateCapsOnUpdate(perSpinCap = perSpinCap, dailyCap = dailyCap)?.let { return it }

        val delegate = hotDelegate?.let { publicKey(it).successOr { failure -> return failure } }
        val vault = vaultAddress().successOr { return it }

        val updateVault = GachaUpdateVaultInstruction.create(
            vault = vault,
            coldOwner = coldOwner,
            hotDelegate = delegate,
            perSpinCap = perSpinCap,
            dailyCap = dailyCap,
        )

        val transaction = transactionBuilder.buildUnsignedTransaction(
            instructions = listOf(updateVault),
            computeUnitLimit = SolanaTransactionBuilder.DEFAULT_CU_LIMIT,
            computeUnitPrice = SolanaTransactionBuilder.DEFAULT_CU_PRICE,
        ).successOr { return it }

        return Result.Success(transaction.toCompiled())
    }

    override suspend fun buildCloseCollectiblesVault(): Result<TransactionData.Compiled> {
        val vault = vaultAddress().successOr { return it }
        val mint = usdcMint().successOr { return it }
        val vaultUsdc = vaultUsdcAddress(vault = vault, mint = mint).successOr { return it }

        val instructions = buildList {
            // Skipped when the account is already gone: `3012` from this instruction would take `close_vault` with it.
            if (isVaultUsdcExist(vaultUsdc).successOr { return it }) {
                add(
                    GachaCloseTokenAccountInstruction.create(
                        vault = vault,
                        coldOwner = coldOwner,
                        tokenAccount = vaultUsdc,
                    ),
                )
            }

            add(GachaCloseVaultInstruction.create(vault = vault, coldOwner = coldOwner))
        }

        val transaction = transactionBuilder.buildUnsignedTransaction(
            instructions = instructions,
            computeUnitLimit = SolanaTransactionBuilder.DEFAULT_CU_LIMIT,
            computeUnitPrice = SolanaTransactionBuilder.DEFAULT_CU_PRICE,
        ).successOr { return it }

        return Result.Success(transaction.toCompiled())
    }

    override suspend fun sendPurchasePack(
        hotDelegate: String,
        amount: Long,
        feeAmount: Long,
        memo: String,
        signer: suspend (message: ByteArray) -> Result<ByteArray>,
    ): Result<TransactionSendResult> {
        val chainMemo = memo + OPEN_PACK_MEMO_SUFFIX

        // An empty memo still passes on chain as ":open": the vault pays, the gacha credits no spin
        if (memo.isEmpty() || chainMemo.toByteArray(Charsets.UTF_8).size > MAX_MEMO_BYTES) {
            return Result.Failure(BlockchainSdkError.CustomError("Invalid memo"))
        }

        val delegate = publicKey(hotDelegate).successOr { return it }
        val config = config().successOr { return it }

        val purchasePack = purchasePackInstruction(
            config = config,
            hotDelegate = delegate,
            amount = amount,
            feeAmount = feeAmount,
            memo = chainMemo,
        ).successOr { return it }

        // The memo is top level and precedes `purchase_pack`: the gacha counts a spin by it, not by the argument
        val transaction = SolanaTransactionBuilder(
            account = delegate,
            multiNetworkProvider = multiNetworkProvider
        )
            .buildUnsignedTransaction(
                instructions = listOf(
                    SolanaMemoProgram.createMemoInstruction(chainMemo),
                    purchasePack
                ),
                computeUnitLimit = SolanaTransactionBuilder.DEFAULT_CU_LIMIT,
                computeUnitPrice = SolanaTransactionBuilder.DEFAULT_CU_PRICE,
            )
            .successOr { return it }

        val startSendingTimestamp = SystemClock.elapsedRealtime()
        val signature = signer(transaction.getSerializedMessage()).successOr { return it }
        val signedTransaction = SolanaTransactionHelper.putSignature(
            transaction = transaction.toWireBytes(),
            signerPublicKey = delegate.toByteArray(),
            signature = signature,
        ).successOr { return it }

        return multiNetworkProvider
            .performRequest { sendTransaction(signedTransaction, startSendingTimestamp) }
            .map(::TransactionSendResult)
    }

    private fun purchasePackInstruction(
        config: GachaConfig,
        hotDelegate: PublicKey,
        amount: Long,
        feeAmount: Long,
        memo: String,
    ): Result<TransactionInstruction> {
        val vault = vaultAddress().successOr { return it }
        val usdcMint = publicKey(config.usdcMint).successOr { return it }
        val gachaUsdc = publicKey(config.gachaUsdcAccount).successOr { return it }
        val feeUsdc = publicKey(config.feeUsdcAccount).successOr { return it }
        val vaultUsdc = vaultUsdcAddress(vault = vault, mint = usdcMint).successOr { return it }
        val configAddress = configAddress().successOr { return it }

        return Result.Success(
            GachaPurchasePackInstruction.create(
                config = configAddress,
                vault = vault,
                sessionKey = hotDelegate,
                usdcMint = usdcMint,
                vaultUsdc = vaultUsdc,
                gachaUsdc = gachaUsdc,
                feeUsdc = feeUsdc,
                amount = amount,
                feeAmount = feeAmount,
                memo = memo,
            ),
        )
    }

    /**
     * Builds the idempotent creation of the vault's USDC token account.
     *
     * The card pays, and being the payer is the only signature the ATA program asks for — the vault owns the account
     * but does not authorise its creation.
     */
    private suspend fun createVaultUsdc(vault: PublicKey): Result<TransactionInstruction> {
        val mint = usdcMint().successOr { return it }
        val vaultUsdc = vaultUsdcAddress(vault = vault, mint = mint).successOr { return it }

        return Result.Success(
            SolanaAssociatedTokenProgram.createIdempotentInstruction(
                payer = coldOwner,
                associatedAccount = vaultUsdc,
                owner = vault,
                mint = mint,
                tokenProgramId = SolanaTokenProgram.ID.TOKEN,
            ),
        )
    }

    /**
     * Whether the vault's USDC account is there to be closed. A missing account is a plain `false`: it means the
     * onboarding bundle never completed, or the account has already been closed — neither is an error here.
     */
    private suspend fun isVaultUsdcExist(vaultUsdc: PublicKey): Result<Boolean> {
        val result = multiNetworkProvider.performRequest { getTokenAccountInfoIfExist(vaultUsdc) }

        return when (result) {
            is Result.Success -> Result.Success(true)
            is Result.Failure -> if (result.error is BlockchainSdkError.AccountNotFound) {
                Result.Success(false)
            } else {
                result
            }
        }
    }

    /** The mint comes from [config] rather than a constant, so it is whatever the program actually uses today. */
    private suspend fun usdcMint(): Result<PublicKey> {
        val usdcMint = config().successOr { return it }.usdcMint

        return runCatching { PublicKey(usdcMint) }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Failure(BlockchainSdkError.CustomError("Config holds an invalid USDC mint")) },
        )
    }

    /**
     * Neither cap updated: nothing to check, the stored pair already satisfies the invariant.
     * Both caps updated: checked against each other.
     * One cap updated: the stored pair is read, since the program checks the merged state.
     */
    private suspend fun validateCapsOnUpdate(perSpinCap: Long?, dailyCap: Long?): Result.Failure? {
        if (perSpinCap == null && dailyCap == null) return null
        if (perSpinCap != null && dailyCap != null) return validateCaps(
            perSpinCap = perSpinCap,
            dailyCap = dailyCap
        )

        val stored = getCollectiblesVault().successOr { return it }
            ?: return Result.Failure(BlockchainSdkError.CustomError("Vault does not exist"))

        return validateCaps(
            perSpinCap = perSpinCap ?: stored.perSpinCap,
            dailyCap = dailyCap ?: stored.dailyCap,
        )
    }

    /**
     * Mirrors the program's own invariant, checked here so it fails before the card is asked for a signature; on-chain
     * the same violation surfaces as `6004 InvalidCaps`.
     */
    private fun validateCaps(perSpinCap: Long, dailyCap: Long): Result.Failure? {
        if (perSpinCap in 1..dailyCap) return null

        return Result.Failure(
            BlockchainSdkError.CustomError("perSpinCap must be greater than 0 and not exceed dailyCap"),
        )
    }

    private fun vaultUsdcAddress(vault: PublicKey, mint: PublicKey): Result<PublicKey> =
        GachaVaultProgram.vaultUsdcAddress(vault = vault, usdcMint = mint).fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Failure(BlockchainSdkError.CustomError("Failed to derive the vault USDC address")) },
        )

    /**
     * Reads the program's `Config` account, the only source of the USDC mint, the treasuries and the fee.
     *
     * Deliberately not cached. The admin changes those values with `update_config` and no redeploy, and a stale copy
     * surfaces on chain as `2012` — an error about a constraint on an account we passed, not about being out of date.
     *
     * Unlike a missing `Vault`, which legitimately means "this wallet was never onboarded" and is reported as null, a
     * missing or foreign `Config` is an ops incident: the program is not where the SDK thinks it is.
     */
    private suspend fun config(): Result<GachaConfig> {
        val address = configAddress().successOr { return it }

        val accountInfo = multiNetworkProvider.performRequest { getAccountInfoIfExist(address) }
            .successOr { failure ->
                return if (failure.error is BlockchainSdkError.AccountNotFound) {
                    Result.Failure(BlockchainSdkError.CustomError("Gacha config account does not exist"))
                } else {
                    failure
                }
            }

        if (accountInfo.owner != GachaVaultProgram.programId.toBase58()) {
            return Result.Failure(BlockchainSdkError.CustomError("Config account is not owned by the gacha program"))
        }

        val rawData = accountInfo.data.firstOrNull()
            ?: return Result.Failure(BlockchainSdkError.CustomError("Config account has no data"))

        val parsed = runCatching { GachaConfigParser.parse(rawData.decodeBase64Bytes()) }
            .getOrNull()
            ?: return Result.Failure(BlockchainSdkError.CustomError("Failed to parse config account"))

        return Result.Success(parsed)
    }

    /**
     * Wire bytes for `send(TransactionData, signer)`: solanaj serializes only signatures it already holds, so the
     * empty slots the card signature is spliced into are reserved by hand. Their count is the first byte of the
     * header — true for the legacy messages the builder compiles, not for v0, which opens with a `0x80` prefix.
     */
    private fun SolanaTransaction.toCompiled(): TransactionData.Compiled =
        TransactionData.Compiled(value = TransactionData.Compiled.Data.Bytes(toWireBytes()))

    private fun SolanaTransaction.toWireBytes(): ByteArray {
        val message = getSerializedMessage()
        val signatureCount = message.first().toInt() and BYTE_MASK

        return byteArrayOf(signatureCount.toByte()) + ByteArray(signatureCount * SIGNATURE_LENGTH) + message
    }

    private fun configAddress(): Result<PublicKey> = GachaVaultProgram.configAddress().fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Failure(BlockchainSdkError.CustomError("Failed to derive config address")) },
    )

    private fun publicKey(address: String): Result<PublicKey> =
        runCatching { PublicKey(address) }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Failure(BlockchainSdkError.CustomError("Invalid public key")) },
        )

    private fun vaultAddress(): Result<PublicKey> = GachaVaultProgram.vaultAddress(coldOwner).fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Failure(BlockchainSdkError.CustomError("Failed to derive vault address")) },
    )

    private companion object {
        const val SIGNATURE_LENGTH = 64
        const val BYTE_MASK = 0xFF
        const val OPEN_PACK_MEMO_SUFFIX = ":open"
        const val MAX_MEMO_BYTES = 256
    }
}