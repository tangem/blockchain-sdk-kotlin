package com.tangem.blockchain.blockchains.solana.nft

import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedSolanaTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program

internal sealed interface SolanaNFTTransferRoute {

    /** A non-programmable SPL or Token-2022 NFT, sent by the regular token transfer. */
    data object Spl : SolanaNFTTransferRoute

    data class Program(
        val instructions: List<TransactionInstruction>,
        val computeUnitLimit: Int,
        val createdAccountSpaces: List<Long>,
        val isCore: Boolean,
    ) : SolanaNFTTransferRoute
}

/**
 * Picks the transfer path of an NFT by the program that owns its account, not by the NFT API: a Core asset is an
 * account of the Core program, a pNFT is a Token mint whose metadata says programmable. Transfers that the chain
 * would reject are refused here, before the card is tapped.
 */
internal class SolanaNFTTransferBuilder(
    private val account: PublicKey,
    private val networkService: SolanaNFTNetworkService,
) {

    suspend fun route(assetAddress: String, destinationAddress: String): Result<SolanaNFTTransferRoute> {
        val asset = assetAddress.toPublicKey() ?: return failure(BlockchainSdkError.Solana.NFTUnsupportedAsset)
        val destination = destinationAddress.toPublicKey()
            ?: return failure(BlockchainSdkError.Solana.NFTInvalidDestination)
        val assetAccount = networkService.readAccount(asset).successOr { return it }
            ?: return failure(BlockchainSdkError.Solana.NFTUnsupportedAsset)

        return when (assetAccount.owner) {
            MplCoreProgram.programId.toBase58() -> coreTransfer(asset, assetAccount.data, destination)
            else -> SolanaTokenProgram.ID.entries.firstOrNull { it.value.toBase58() == assetAccount.owner }
                ?.let { tokenProgram -> tokenTransfer(asset, destination, tokenProgram) }
                ?: failure(BlockchainSdkError.Solana.NFTUnsupportedAsset)
        }
    }

    private suspend fun coreTransfer(
        asset: PublicKey,
        assetData: ByteArray,
        destination: PublicKey,
    ): Result<SolanaNFTTransferRoute> {
        if (destination == asset) return failure(BlockchainSdkError.Solana.NFTInvalidDestination)

        val parsed = MplCoreAccountParser.parseAsset(assetData)
            ?: return failure(BlockchainSdkError.Solana.NFTUnsupportedAsset)
        if (parsed.owner != account) return failure(BlockchainSdkError.Solana.NFTNotOwner)
        checkTransferBlock(parsed.transferBlock)?.let { return it }
        checkDestination(networkService.readAccount(destination))?.let { return it }

        if (parsed.collection != null) {
            val collection = networkService.readAccount(parsed.collection).successOr { return it }
            if (collection == null || collection.owner != MplCoreProgram.programId.toBase58()) {
                return failure(BlockchainSdkError.Solana.NFTUnsupportedAsset)
            }
            val collectionBlock = MplCoreAccountParser.parseCollectionTransferBlock(collection.data)
                .getOrElse { return failure(BlockchainSdkError.Solana.NFTUnsupportedAsset) }
            checkTransferBlock(collectionBlock)?.let { return it }
        }

        return Result.Success(
            SolanaNFTTransferRoute.Program(
                instructions = listOf(
                    MplCoreProgram.transferV1(
                        asset = asset,
                        collection = parsed.collection,
                        payer = account,
                        newOwner = destination,
                    ),
                ),
                computeUnitLimit = CORE_COMPUTE_UNIT_LIMIT,
                createdAccountSpaces = emptyList(),
                isCore = true,
            ),
        )
    }

    private suspend fun tokenTransfer(
        mint: PublicKey,
        destination: PublicKey,
        tokenProgram: SolanaTokenProgram.ID,
    ): Result<SolanaNFTTransferRoute> {
        val metadataAccount = networkService.readAccount(TokenMetadataProgram.metadata(mint)).successOr { return it }
        val metadata = metadataAccount
            ?.takeIf { it.owner == TokenMetadataProgram.programId.toBase58() }
            ?.let { TokenMetadataParser.parse(it.data) }
        if (metadata == null || !metadata.isProgrammable) return Result.Success(SolanaNFTTransferRoute.Spl)

        return programmableTransfer(
            mint = mint,
            ruleSet = metadata.ruleSet,
            destination = destination,
            tokenProgram = tokenProgram,
        )
    }

    private suspend fun programmableTransfer(
        mint: PublicKey,
        ruleSet: PublicKey?,
        destination: PublicKey,
        tokenProgram: SolanaTokenProgram.ID,
    ): Result<SolanaNFTTransferRoute> {
        if (destination == account) return failure(BlockchainSdkError.Solana.NFTInvalidDestination)

        val ownerToken = associatedTokenAccount(account, mint, tokenProgram).successOr { return it }
        val destinationToken = associatedTokenAccount(destination, mint, tokenProgram).successOr { return it }

        val reads = coroutineScope {
            listOf(
                async { networkService.readAccount(TokenMetadataProgram.tokenRecord(mint, ownerToken)) },
                async { networkService.readAccount(destination) },
                async { networkService.readAccount(destinationToken) },
                async { networkService.readAccount(TokenMetadataProgram.tokenRecord(mint, destinationToken)) },
            ).awaitAll()
        }
        val (ownerRecordResult, destinationResult, destinationTokenResult) = reads
        val destinationRecordResult = reads[DESTINATION_RECORD_READ]

        val ownerRecord = ownerRecordResult.successOr { return it }
            ?: return failure(BlockchainSdkError.Solana.NFTNotOwner)
        when (TokenMetadataParser.parseTokenRecordState(ownerRecord.data)) {
            TokenRecordState.Unlocked -> Unit
            TokenRecordState.Locked, TokenRecordState.Listed -> return failure(BlockchainSdkError.Solana.NFTLocked)
            null -> return failure(BlockchainSdkError.Solana.NFTUnsupportedAsset)
        }
        checkDestination(destinationResult)?.let { return it }

        val destinationTokenAccount = destinationTokenResult.successOr { return it }
        val destinationRecord = destinationRecordResult.successOr { return it }

        return Result.Success(
            SolanaNFTTransferRoute.Program(
                instructions = listOf(
                    TokenMetadataProgram.transfer(
                        mint = mint,
                        owner = account,
                        ownerToken = ownerToken,
                        destinationOwner = destination,
                        destinationToken = destinationToken,
                        ruleSet = ruleSet,
                        tokenProgram = tokenProgram.value,
                    ),
                ),
                computeUnitLimit = PROGRAMMABLE_COMPUTE_UNIT_LIMIT,
                createdAccountSpaces = listOfNotNull(
                    tokenAccountSpace(tokenProgram).takeIf { destinationTokenAccount == null },
                    TOKEN_RECORD_SPACE.takeIf { destinationRecord == null },
                ),
                isCore = false,
            ),
        )
    }

    /**
     * Both programs make the recipient the new owner as is, and nobody can sign for a token account, a mint or
     * another program's account, so an NFT sent there is lost. Only a wallet passes: a system account or none yet.
     */
    private fun checkDestination(destination: Result<SolanaNFTAccount?>): Result.Failure? {
        val account = destination.successOr { return it } ?: return null
        return if (account.owner == Program.Id.system.toBase58()) {
            null
        } else {
            failure(BlockchainSdkError.Solana.NFTInvalidDestination)
        }
    }

    private fun checkTransferBlock(block: MplCoreTransferBlock?): Result.Failure? = when (block) {
        null -> null
        MplCoreTransferBlock.Frozen -> failure(BlockchainSdkError.Solana.NFTFrozen)
        MplCoreTransferBlock.ExternalCheck -> failure(BlockchainSdkError.Solana.NFTUnsupportedAsset)
    }

    private fun associatedTokenAccount(
        owner: PublicKey,
        mint: PublicKey,
        tokenProgram: SolanaTokenProgram.ID,
    ): Result<PublicKey> = createAssociatedSolanaTokenAddress(owner, mint, tokenProgram)
        .fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Failure(BlockchainSdkError.Solana.FailedToCreateAssociatedAccount) },
        )

    /** A Token-2022 associated account carries the mandatory `ImmutableOwner` extension. */
    private fun tokenAccountSpace(tokenProgram: SolanaTokenProgram.ID): Long = when (tokenProgram) {
        SolanaTokenProgram.ID.TOKEN -> TOKEN_ACCOUNT_SPACE
        SolanaTokenProgram.ID.TOKEN_2022 -> TOKEN_2022_ACCOUNT_SPACE
    }

    private fun failure(error: BlockchainSdkError) = Result.Failure(error)

    private fun String.toPublicKey(): PublicKey? = runCatching { PublicKey(this) }.getOrNull()

    private companion object {
        const val CORE_COMPUTE_UNIT_LIMIT = 50_000
        const val DESTINATION_RECORD_READ = 3
        const val PROGRAMMABLE_COMPUTE_UNIT_LIMIT = 200_000
        const val TOKEN_ACCOUNT_SPACE = 165L
        const val TOKEN_2022_ACCOUNT_SPACE = 170L
        const val TOKEN_RECORD_SPACE = 80L
    }
}