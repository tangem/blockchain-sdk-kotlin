package com.tangem.blockchain.transactionhistory.blockchains.solana

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.transactionhistory.blockchains.solana.network.*
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.*
import java.math.BigDecimal

@Suppress("LargeClass")
internal class SolanaTransactionHistoryMapper(
    private val blockchain: Blockchain,
) {

    @Suppress("CyclomaticComplexMethod")
    fun mapToHistoryItem(
        signatureInfo: SolanaSignatureInfo,
        txResponse: SolanaTransactionResponse,
        walletAddress: String,
        filterToken: Token?,
    ): TransactionHistoryItem? {
        val meta = txResponse.meta ?: return null
        val message = txResponse.transaction?.message ?: return null
        val accountKeys = message.accountKeys.map { it.pubkey }
        val walletIndex = accountKeys.indexOfFirst { it.equals(walletAddress, ignoreCase = true) }

        val allInstructions = collectAllInstructions(message, meta)
        val txType = classifyTransaction(meta, allInstructions)

        val signature = txResponse.transaction.signatures.firstOrNull() ?: signatureInfo.signature
        val timestamp = (txResponse.blockTime ?: signatureInfo.blockTime ?: 0L) * MILLIS_IN_SECOND

        val status = when {
            signatureInfo.err != null || meta.err != null -> TransactionStatus.Failed
            signatureInfo.confirmationStatus == FINALIZED_STATUS ||
                signatureInfo.confirmationStatus == CONFIRMED_STATUS -> TransactionStatus.Confirmed
            else -> TransactionStatus.Unconfirmed
        }

        return when (txType) {
            is SolanaTxType.StakeOperation -> {
                if (filterToken != null) return null
                mapStakeOperation(
                    txHash = signature,
                    timestamp = timestamp,
                    status = status,
                    txType = txType,
                    meta = meta,
                    walletAddress = walletAddress,
                    walletIndex = walletIndex,
                )
            }
            is SolanaTxType.Transfer -> {
                if (filterToken != null) return null
                mapSolTransfer(
                    txHash = signature,
                    timestamp = timestamp,
                    status = status,
                    txType = txType,
                    meta = meta,
                    walletIndex = walletIndex,
                )
            }
            is SolanaTxType.TokenTransfer -> {
                mapTokenTransfer(
                    txHash = signature,
                    timestamp = timestamp,
                    status = status,
                    txType = txType,
                    meta = meta,
                    walletAddress = walletAddress,
                    accountKeys = accountKeys,
                    filterToken = filterToken,
                )
            }
            is SolanaTxType.OtherOperation -> {
                if (filterToken != null) return null
                mapOtherOperation(
                    txHash = signature,
                    timestamp = timestamp,
                    status = status,
                    meta = meta,
                    walletAddress = walletAddress,
                    walletIndex = walletIndex,
                )
            }
        }
    }

    private fun collectAllInstructions(
        message: SolanaTransactionMessage,
        meta: SolanaTransactionMeta,
    ): List<SolanaInstruction> {
        val topLevel = message.instructions
        val inner = meta.innerInstructions?.flatMap { it.instructions }.orEmpty()
        return topLevel + inner
    }

    @Suppress("CyclomaticComplexMethod")
    private fun classifyTransaction(
        meta: SolanaTransactionMeta,
        allInstructions: List<SolanaInstruction>,
    ): SolanaTxType {
        // STAKING: check for Stake program instructions first
        val stakeInstruction = allInstructions.firstOrNull {
            it.programId == STAKE_PROGRAM_ID || it.program == STAKE_PROGRAM
        }
        if (stakeInstruction != null) {
            val info = stakeInstruction.parsed?.info
            return SolanaTxType.StakeOperation(
                stakeType = stakeInstruction.parsed?.type,
                voteAccount = info?.voteAccount,
                stakeAccount = info?.stakeAccount,
            )
        }

        // TRANSFER: innerInstructions empty, preTokenBalances/postTokenBalances/rewards empty,
        // all programIds are system or computeBudget, has transfer instruction with system programId
        val isSimpleTransaction = meta.innerInstructions.isNullOrEmpty() &&
            meta.preTokenBalances.isNullOrEmpty() &&
            meta.postTokenBalances.isNullOrEmpty() &&
            meta.rewards.isNullOrEmpty()
        if (isSimpleTransaction) {
            val allProgramIds = allInstructions.mapNotNull { it.programId }.toSet()
            val hasOnlySystemPrograms = allProgramIds.all {
                it == SYSTEM_PROGRAM_ID || it.startsWith(COMPUTE_BUDGET_PREFIX)
            }
            if (hasOnlySystemPrograms) {
                val transferInstruction = allInstructions.firstOrNull {
                    it.programId == SYSTEM_PROGRAM_ID && it.parsed?.type == TRANSFER_TYPE
                }
                if (transferInstruction != null) {
                    val info = transferInstruction.parsed?.info
                    return SolanaTxType.Transfer(
                        source = info?.source.orEmpty(),
                        destination = info?.destination.orEmpty(),
                        lamports = info?.lamports ?: 0L,
                    )
                }
            }
        }

        // TOKEN TRANSFER: usedPrograms has spl-token with type "transfer" or "transferChecked"
        val tokenTransferInstruction = allInstructions.firstOrNull { instruction ->
            instruction.program == SPL_TOKEN_PROGRAM &&
                (instruction.parsed?.type == TRANSFER_TYPE || instruction.parsed?.type == TRANSFER_CHECKED_TYPE)
        }
        if (tokenTransferInstruction != null) {
            val info = tokenTransferInstruction.parsed?.info
            return SolanaTxType.TokenTransfer(
                mint = info?.mint.orEmpty(),
                source = info?.source.orEmpty(),
                destination = info?.destination.orEmpty(),
                authority = info?.authority,
                rawAmount = info?.amount ?: info?.tokenAmount?.amount,
                decimals = info?.tokenAmount?.decimals,
            )
        }

        // OTHER OPERATION: fallback for unrecognized transactions
        return SolanaTxType.OtherOperation
    }

    @Suppress("LongParameterList")
    private fun mapSolTransfer(
        txHash: String,
        timestamp: Long,
        status: TransactionStatus,
        txType: SolanaTxType.Transfer,
        meta: SolanaTransactionMeta,
        walletIndex: Int,
    ): TransactionHistoryItem {
        val solDelta = calculateSolDelta(meta, walletIndex)
        val isOutgoing = solDelta < 0
        val solAmount = BigDecimal(kotlin.math.abs(solDelta)).movePointLeft(blockchain.decimals())

        return TransactionHistoryItem(
            txHash = txHash,
            timestamp = timestamp,
            isOutgoing = isOutgoing,
            destinationType = DestinationType.Single(AddressType.User(txType.destination)),
            sourceType = SourceType.Single(txType.source),
            status = status,
            type = TransactionType.Transfer,
            amount = Amount(value = solAmount, blockchain = blockchain),
        )
    }

    @Suppress("LongParameterList")
    private fun mapStakeOperation(
        txHash: String,
        timestamp: Long,
        status: TransactionStatus,
        txType: SolanaTxType.StakeOperation,
        meta: SolanaTransactionMeta,
        walletAddress: String,
        walletIndex: Int,
    ): TransactionHistoryItem {
        val solDelta = calculateSolDelta(meta, walletIndex)
        val isOutgoing = solDelta < 0
        val solAmount = BigDecimal(kotlin.math.abs(solDelta)).movePointLeft(blockchain.decimals())

        val transactionType = when (txType.stakeType) {
            DELEGATE_TYPE -> TransactionType.SolanaStakingTransactionType.Stake(
                validatorAddress = txType.voteAccount,
            )
            DEACTIVATE_TYPE -> TransactionType.SolanaStakingTransactionType.Unstake
            else -> TransactionType.SolanaStakingTransactionType.Stake(
                validatorAddress = txType.voteAccount,
            )
        }

        val destinationType = if (txType.voteAccount != null) {
            DestinationType.Single(AddressType.Validator(txType.voteAccount))
        } else {
            DestinationType.Single(AddressType.User(txType.stakeAccount ?: walletAddress))
        }

        return TransactionHistoryItem(
            txHash = txHash,
            timestamp = timestamp,
            isOutgoing = isOutgoing,
            destinationType = destinationType,
            sourceType = SourceType.Single(walletAddress),
            status = status,
            type = transactionType,
            amount = Amount(value = solAmount, blockchain = blockchain),
        )
    }

    @Suppress("LongParameterList")
    private fun mapTokenTransfer(
        txHash: String,
        timestamp: Long,
        status: TransactionStatus,
        txType: SolanaTxType.TokenTransfer,
        meta: SolanaTransactionMeta,
        walletAddress: String,
        accountKeys: List<String>,
        filterToken: Token?,
    ): TransactionHistoryItem? {
        // Find all mints involved for this wallet
        val walletMints = collectWalletTokenMints(meta, walletAddress)

        // If filtering by token, only process matching mint
        if (filterToken != null && !walletMints.any { it.equals(filterToken.contractAddress, ignoreCase = true) }) {
            return null
        }

        // Calculate token balance change
        val targetMint = if (filterToken != null) {
            filterToken.contractAddress
        } else {
            // For coin filter, use the first non-empty mint we find for this wallet
            txType.mint.ifEmpty { walletMints.firstOrNull() } ?: return null
        }

        val tokenDelta = calculateTokenDelta(meta, walletAddress, targetMint)
        val isOutgoing = tokenDelta < 0
        val decimals = txType.decimals
            ?: findDecimals(meta, targetMint)
            ?: 0
        val tokenAmount = BigDecimal(kotlin.math.abs(tokenDelta)).movePointLeft(decimals)

        val token = filterToken ?: Token(
            symbol = "",
            name = "",
            contractAddress = targetMint,
            decimals = decimals,
        )

        // Resolve token account addresses to wallet addresses
        val sourceAddress = txType.authority
            ?: resolveTokenAccountOwner(txType.source, meta, accountKeys)
        val destinationAddress = resolveTokenAccountOwner(txType.destination, meta, accountKeys)

        return TransactionHistoryItem(
            txHash = txHash,
            timestamp = timestamp,
            isOutgoing = isOutgoing,
            destinationType = DestinationType.Single(AddressType.User(destinationAddress)),
            sourceType = SourceType.Single(sourceAddress),
            status = status,
            type = TransactionType.Transfer,
            amount = Amount(token = token, value = tokenAmount),
        )
    }

    @Suppress("LongParameterList")
    private fun mapOtherOperation(
        txHash: String,
        timestamp: Long,
        status: TransactionStatus,
        meta: SolanaTransactionMeta,
        walletAddress: String,
        walletIndex: Int,
    ): TransactionHistoryItem {
        val solDelta = calculateSolDelta(meta, walletIndex)
        val isOutgoing = solDelta < 0
        val solAmount = BigDecimal(kotlin.math.abs(solDelta)).movePointLeft(blockchain.decimals())

        return TransactionHistoryItem(
            txHash = txHash,
            timestamp = timestamp,
            isOutgoing = isOutgoing,
            destinationType = DestinationType.Single(AddressType.User(walletAddress)),
            sourceType = SourceType.Single(walletAddress),
            status = status,
            type = TransactionType.ContractMethodName(name = OPERATION_TYPE_NAME),
            amount = Amount(value = solAmount, blockchain = blockchain),
        )
    }

    private fun calculateSolDelta(meta: SolanaTransactionMeta, walletIndex: Int): Long {
        if (walletIndex < 0) return 0L
        val pre = meta.preBalances.getOrElse(walletIndex) { 0L }
        val post = meta.postBalances.getOrElse(walletIndex) { 0L }
        return post - pre
    }

    private fun calculateTokenDelta(meta: SolanaTransactionMeta, walletAddress: String, mint: String): Long {
        val preAmount = meta.preTokenBalances.orEmpty()
            .filter { it.owner.equals(walletAddress, ignoreCase = true) && it.mint.equals(mint, ignoreCase = true) }
            .sumOf { it.uiTokenAmount?.amount?.toLongOrNull() ?: 0L }
        val postAmount = meta.postTokenBalances.orEmpty()
            .filter { it.owner.equals(walletAddress, ignoreCase = true) && it.mint.equals(mint, ignoreCase = true) }
            .sumOf { it.uiTokenAmount?.amount?.toLongOrNull() ?: 0L }
        return postAmount - preAmount
    }

    private fun collectWalletTokenMints(meta: SolanaTransactionMeta, walletAddress: String): Set<String> {
        val pre = meta.preTokenBalances.orEmpty()
            .filter { it.owner.equals(walletAddress, ignoreCase = true) }
            .map { it.mint }
        val post = meta.postTokenBalances.orEmpty()
            .filter { it.owner.equals(walletAddress, ignoreCase = true) }
            .map { it.mint }
        return (pre + post).toSet()
    }

    private fun resolveTokenAccountOwner(
        tokenAccountAddress: String,
        meta: SolanaTransactionMeta,
        accountKeys: List<String>,
    ): String {
        val allTokenBalances = meta.postTokenBalances.orEmpty() + meta.preTokenBalances.orEmpty()
        for (balance in allTokenBalances) {
            val pubkey = accountKeys.getOrNull(balance.accountIndex)
            if (pubkey.equals(tokenAccountAddress, ignoreCase = true) && !balance.owner.isNullOrEmpty()) {
                return balance.owner
            }
        }
        return tokenAccountAddress
    }

    private fun findDecimals(meta: SolanaTransactionMeta, mint: String): Int? {
        return meta.postTokenBalances.orEmpty()
            .firstOrNull { it.mint.equals(mint, ignoreCase = true) }
            ?.uiTokenAmount?.decimals
            ?: meta.preTokenBalances.orEmpty()
                .firstOrNull { it.mint.equals(mint, ignoreCase = true) }
                ?.uiTokenAmount?.decimals
    }

    private sealed class SolanaTxType {
        data class Transfer(
            val source: String,
            val destination: String,
            val lamports: Long,
        ) : SolanaTxType()

        data class TokenTransfer(
            val mint: String,
            val source: String,
            val destination: String,
            val authority: String?,
            val rawAmount: String?,
            val decimals: Int?,
        ) : SolanaTxType()

        data class StakeOperation(
            val stakeType: String?,
            val voteAccount: String?,
            val stakeAccount: String?,
        ) : SolanaTxType()

        data object OtherOperation : SolanaTxType()
    }

    private companion object {
        const val SYSTEM_PROGRAM_ID = "11111111111111111111111111111111"
        const val SYSTEM_PROGRAM = "system"
        const val COMPUTE_BUDGET_PREFIX = "ComputeBudget"
        const val SPL_TOKEN_PROGRAM = "spl-token"
        const val STAKE_PROGRAM_ID = "Stake11111111111111111111111111111111111111"
        const val STAKE_PROGRAM = "stake"
        const val TRANSFER_TYPE = "transfer"
        const val TRANSFER_CHECKED_TYPE = "transferChecked"
        const val DELEGATE_TYPE = "delegate"
        const val DEACTIVATE_TYPE = "deactivate"
        const val CONFIRMED_STATUS = "confirmed"
        const val FINALIZED_STATUS = "finalized"
        const val OPERATION_TYPE_NAME = "Operation"
        const val MILLIS_IN_SECOND = 1000L
    }
}