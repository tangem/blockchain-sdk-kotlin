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
        val allInstructions = collectAllInstructions(message, meta)
        val txType = classifyTransaction(meta, allInstructions)
        val context = HistoryItemContext(
            txHash = txResponse.transaction.signatures.firstOrNull() ?: signatureInfo.signature,
            timestamp = (txResponse.blockTime ?: signatureInfo.blockTime ?: 0L) * MILLIS_IN_SECOND,
            status = resolveStatus(signatureInfo, meta),
            meta = meta,
            walletAddress = walletAddress,
            accountKeys = accountKeys,
        )

        val amountData = calculateAmountDelta(
            meta = meta,
            walletIndex = accountKeys.indexOfFirst { it.equals(walletAddress, ignoreCase = true) },
            walletAddress = walletAddress,
            accountKeys = accountKeys,
            filterToken = filterToken,
        ) ?: return null

        return mapTransactionByType(
            txType = txType,
            context = context,
            filterToken = filterToken,
            isOutgoing = amountData.first,
            amount = amountData.second,
        )
    }

    @Suppress("ParameterListWrapping")
    private fun mapTransactionByType(
        txType: SolanaTxType,
        context: HistoryItemContext,
        filterToken: Token?,
        isOutgoing: Boolean,
        amount: Amount,
    ): TransactionHistoryItem? {
        return when (txType) {
            is SolanaTxType.StakeOperation -> mapStakeOperation(
                txHash = context.txHash,
                timestamp = context.timestamp,
                status = context.status,
                txType = txType,
                meta = context.meta,
                walletAddress = context.walletAddress,
                accountKeys = context.accountKeys,
                isOutgoing = isOutgoing,
                amount = amount,
            )
            is SolanaTxType.Transfer -> {
                if (filterToken != null) return null
                mapSolTransfer(
                    txHash = context.txHash,
                    timestamp = context.timestamp,
                    status = context.status,
                    txType = txType,
                    meta = context.meta,
                    walletAddress = context.walletAddress,
                    accountKeys = context.accountKeys,
                    isOutgoing = isOutgoing,
                    amount = amount,
                )
            }
            is SolanaTxType.TokenTransfer -> {
                if (filterToken == null) {
                    mapOtherOperation(
                        txHash = context.txHash,
                        timestamp = context.timestamp,
                        status = context.status,
                        walletAddress = context.walletAddress,
                        accountKeys = context.accountKeys,
                        meta = context.meta,
                        isOutgoing = isOutgoing,
                        amount = amount,
                    )
                } else {
                    mapTokenTransfer(
                        txHash = context.txHash,
                        timestamp = context.timestamp,
                        status = context.status,
                        txType = txType,
                        meta = context.meta,
                        walletAddress = context.walletAddress,
                        accountKeys = context.accountKeys,
                        isOutgoing = isOutgoing,
                        amount = amount,
                    )
                }
            }
            is SolanaTxType.OtherOperation -> {
                if (filterToken != null) return null
                mapOtherOperation(
                    txHash = context.txHash,
                    timestamp = context.timestamp,
                    status = context.status,
                    walletAddress = context.walletAddress,
                    accountKeys = context.accountKeys,
                    meta = context.meta,
                    isOutgoing = isOutgoing,
                    amount = amount,
                )
            }
        }
    }

    private fun resolveStatus(signatureInfo: SolanaSignatureInfo, meta: SolanaTransactionMeta): TransactionStatus {
        return when {
            signatureInfo.err != null || meta.err != null -> TransactionStatus.Failed
            signatureInfo.confirmationStatus == FINALIZED_STATUS ||
                signatureInfo.confirmationStatus == CONFIRMED_STATUS -> TransactionStatus.Confirmed
            else -> TransactionStatus.Unconfirmed
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
        val stakeInstruction = allInstructions.firstOrNull {
            it.programId == STAKE_PROGRAM_ID || it.program == STAKE_PROGRAM
        }
        if (stakeInstruction != null) {
            val info = stakeInstruction.parsed?.info
            return SolanaTxType.StakeOperation(
                stakeType = stakeInstruction.parsed?.type,
                source = info?.source,
                destination = info?.destination,
                newAccount = info?.newAccount,
                voteAccount = info?.voteAccount,
                stakeAccount = info?.stakeAccount,
            )
        }

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
                        source = info?.source,
                        destination = info?.destination,
                        lamports = info?.lamports ?: 0L,
                    )
                }
            }
        }

        val tokenTransferInstruction = allInstructions.firstOrNull { instruction ->
            instruction.program == SPL_TOKEN_PROGRAM &&
                (instruction.parsed?.type == TRANSFER_TYPE || instruction.parsed?.type == TRANSFER_CHECKED_TYPE)
        }
        if (tokenTransferInstruction != null) {
            val info = tokenTransferInstruction.parsed?.info
            return SolanaTxType.TokenTransfer(
                mint = info?.mint,
                source = info?.source,
                destination = info?.destination,
                authority = info?.authority,
            )
        }

        return SolanaTxType.OtherOperation
    }

    @Suppress("LongParameterList")
    private fun mapSolTransfer(
        txHash: String,
        timestamp: Long,
        status: TransactionStatus,
        txType: SolanaTxType.Transfer,
        meta: SolanaTransactionMeta,
        walletAddress: String,
        accountKeys: List<String>,
        isOutgoing: Boolean,
        amount: Amount,
    ): TransactionHistoryItem {
        val sourceAddress = txType.source?.takeUnless { it.isBlank() } ?: walletAddress
        val destinationAddress = txType.destination.takeUnless { it.isNullOrBlank() }
            ?: fallbackCounterpartyAddress(accountKeys = accountKeys, walletAddress = walletAddress)

        return TransactionHistoryItem(
            txHash = txHash,
            timestamp = timestamp,
            isOutgoing = isOutgoing,
            destinationType = DestinationType.Single(AddressType.User(destinationAddress)),
            sourceType = SourceType.Single(sourceAddress),
            status = status,
            type = TransactionType.Transfer,
            amount = amount,
            fee = extractFeeAmount(meta),
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
        accountKeys: List<String>,
        isOutgoing: Boolean,
        amount: Amount,
    ): TransactionHistoryItem {
        val transactionType = when (txType.stakeType) {
            DELEGATE_TYPE -> TransactionType.SolanaStakingTransactionType.Stake(
                validatorAddress = txType.voteAccount,
            )
            DEACTIVATE_TYPE -> TransactionType.SolanaStakingTransactionType.Unstake
            WITHDRAW_TYPE -> TransactionType.SolanaStakingTransactionType.Withdraw
            else -> TransactionType.SolanaStakingTransactionType.Stake(
                validatorAddress = txType.voteAccount,
            )
        }

        val counterparty = fallbackCounterpartyAddress(accountKeys = accountKeys, walletAddress = walletAddress)
        val sourceAddress = txType.source
            ?: txType.stakeAccount
            ?: if (isOutgoing) walletAddress else counterparty
        val destinationAddress = txType.destination
            ?: txType.newAccount
            ?: if (isOutgoing) counterparty else walletAddress
        val destinationType = if (txType.voteAccount != null) {
            DestinationType.Single(AddressType.Validator(txType.voteAccount))
        } else {
            DestinationType.Single(AddressType.User(destinationAddress))
        }

        return TransactionHistoryItem(
            txHash = txHash,
            timestamp = timestamp,
            isOutgoing = isOutgoing,
            destinationType = destinationType,
            sourceType = SourceType.Single(sourceAddress),
            status = status,
            type = transactionType,
            amount = amount,
            fee = extractFeeAmount(meta),
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
        isOutgoing: Boolean,
        amount: Amount,
    ): TransactionHistoryItem {
        val sourceAddress = txType.authority?.takeUnless { it.isBlank() }
            ?: txType.source?.takeUnless { it.isBlank() }?.let { resolveTokenAccountOwner(it, meta, accountKeys) }
            ?: if (isOutgoing) walletAddress else fallbackCounterpartyAddress(accountKeys, walletAddress)
        val destinationAddress = txType.destination?.takeUnless { it.isBlank() }
            ?.let { resolveTokenAccountOwner(it, meta, accountKeys) }
            ?: if (isOutgoing) fallbackCounterpartyAddress(accountKeys, walletAddress) else walletAddress

        return TransactionHistoryItem(
            txHash = txHash,
            timestamp = timestamp,
            isOutgoing = isOutgoing,
            destinationType = DestinationType.Single(AddressType.User(destinationAddress)),
            sourceType = SourceType.Single(sourceAddress),
            status = status,
            type = TransactionType.Transfer,
            amount = amount,
            fee = extractFeeAmount(meta),
        )
    }

    @Suppress("LongParameterList")
    private fun mapOtherOperation(
        txHash: String,
        timestamp: Long,
        status: TransactionStatus,
        walletAddress: String,
        accountKeys: List<String>,
        meta: SolanaTransactionMeta,
        isOutgoing: Boolean,
        amount: Amount,
    ): TransactionHistoryItem {
        val counterparty = fallbackCounterpartyAddress(accountKeys = accountKeys, walletAddress = walletAddress)
        val sourceAddress = if (isOutgoing) walletAddress else counterparty
        val destinationAddress = if (isOutgoing) counterparty else walletAddress

        return TransactionHistoryItem(
            txHash = txHash,
            timestamp = timestamp,
            isOutgoing = isOutgoing,
            destinationType = DestinationType.Single(AddressType.User(destinationAddress)),
            sourceType = SourceType.Single(sourceAddress),
            status = status,
            type = TransactionType.ContractMethodName(name = OPERATION_TYPE_NAME),
            amount = amount,
            fee = extractFeeAmount(meta),
        )
    }

    @Suppress("CyclomaticComplexMethod")
    private fun calculateAmountDelta(
        meta: SolanaTransactionMeta,
        walletIndex: Int,
        walletAddress: String,
        accountKeys: List<String>,
        filterToken: Token?,
    ): Pair<Boolean, Amount>? {
        if (filterToken == null) {
            val solDelta = calculateSolDelta(meta, walletIndex)
            if (solDelta == 0L) return null
            return Pair(
                solDelta < 0,
                Amount(
                    value = BigDecimal(kotlin.math.abs(solDelta)).movePointLeft(blockchain.decimals()),
                    blockchain = blockchain,
                ),
            )
        } else {
            val tokenDelta = calculateTokenDelta(
                meta = meta,
                walletAddress = walletAddress,
                mint = filterToken.contractAddress,
                accountKeys = accountKeys,
            )
            if (tokenDelta.compareTo(BigDecimal.ZERO) != 0) {
                val isToken2022 = meta.preTokenBalances.orEmpty()
                    .plus(meta.postTokenBalances.orEmpty())
                    .any { balance ->
                        balance.programId == TOKEN_2022_PROGRAM_ID &&
                            balance.mint.equals(filterToken.contractAddress, ignoreCase = true)
                    }
                val value = if (isToken2022) {
                    tokenDelta.abs()
                } else {
                    val decimals = findDecimals(meta, filterToken.contractAddress) ?: filterToken.decimals
                    tokenDelta.abs().movePointLeft(decimals)
                }
                return Pair(
                    tokenDelta < BigDecimal.ZERO,
                    Amount(token = filterToken, value = value),
                )
            }
            return null
        }
    }

    private fun calculateSolDelta(meta: SolanaTransactionMeta, walletIndex: Int): Long {
        if (walletIndex < 0) return 0L
        val pre = meta.preBalances.getOrElse(walletIndex) { 0L }
        val post = meta.postBalances.getOrElse(walletIndex) { 0L }
        return post - pre
    }

    private fun calculateTokenDelta(
        meta: SolanaTransactionMeta,
        walletAddress: String,
        mint: String,
        accountKeys: List<String>,
    ): BigDecimal {
        return sumTokenBalances(
            balances = meta.postTokenBalances,
            walletAddress = walletAddress,
            mint = mint,
            accountKeys = accountKeys,
        ) -
            sumTokenBalances(
                balances = meta.preTokenBalances,
                walletAddress = walletAddress,
                mint = mint,
                accountKeys = accountKeys,
            )
    }

    private fun sumTokenBalances(
        balances: List<SolanaTokenBalance>?,
        walletAddress: String,
        mint: String,
        accountKeys: List<String>,
    ): BigDecimal {
        return balances.orEmpty()
            .filter { it.mint.equals(mint, ignoreCase = true) && it.belongsTo(walletAddress, accountKeys) }
            .sumOf { it.resolveAmount() }
    }

    /**
     * Resolves the token amount from a balance entry.
     * For Token-2022, uses the node-provided `uiAmountString`/`uiAmount` (pre-scaled).
     * For standard SPL tokens, uses the raw `amount` string.
     */
    private fun SolanaTokenBalance.resolveAmount(): BigDecimal {
        val tokenAmount = uiTokenAmount ?: return BigDecimal.ZERO
        val isToken2022 = programId == TOKEN_2022_PROGRAM_ID
        return if (isToken2022) {
            tokenAmount.uiAmountString?.toBigDecimalOrNull()
                ?: tokenAmount.uiAmount?.toBigDecimal()
                ?: BigDecimal.ZERO
        } else {
            tokenAmount.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        }
    }

    private fun SolanaTokenBalance.belongsTo(walletAddress: String, accountKeys: List<String>): Boolean {
        if (owner.equals(walletAddress, ignoreCase = true)) return true
        val pubkey = accountKeys.getOrNull(accountIndex) ?: return false
        return pubkey.equals(walletAddress, ignoreCase = true)
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

    private fun fallbackCounterpartyAddress(accountKeys: List<String>, walletAddress: String): String {
        return accountKeys.firstOrNull { !it.equals(walletAddress, ignoreCase = true) } ?: walletAddress
    }

    private fun extractFeeAmount(meta: SolanaTransactionMeta): Amount {
        return Amount(
            value = BigDecimal(meta.fee).movePointLeft(blockchain.decimals()),
            blockchain = blockchain,
        )
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
            val source: String?,
            val destination: String?,
            val lamports: Long,
        ) : SolanaTxType()

        data class TokenTransfer(
            val mint: String?,
            val source: String?,
            val destination: String?,
            val authority: String?,
        ) : SolanaTxType()

        data class StakeOperation(
            val stakeType: String?,
            val source: String?,
            val destination: String?,
            val newAccount: String?,
            val voteAccount: String?,
            val stakeAccount: String?,
        ) : SolanaTxType()

        data object OtherOperation : SolanaTxType()
    }

    private data class HistoryItemContext(
        val txHash: String,
        val timestamp: Long,
        val status: TransactionStatus,
        val meta: SolanaTransactionMeta,
        val walletAddress: String,
        val accountKeys: List<String>,
    )

    private companion object {
        const val SYSTEM_PROGRAM_ID = "11111111111111111111111111111111"
        const val SYSTEM_PROGRAM = "system"
        const val COMPUTE_BUDGET_PREFIX = "ComputeBudget"
        const val SPL_TOKEN_PROGRAM = "spl-token"
        const val TOKEN_2022_PROGRAM_ID = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"
        const val STAKE_PROGRAM_ID = "Stake11111111111111111111111111111111111111"
        const val STAKE_PROGRAM = "stake"
        const val TRANSFER_TYPE = "transfer"
        const val TRANSFER_CHECKED_TYPE = "transferChecked"
        const val DELEGATE_TYPE = "delegate"
        const val DEACTIVATE_TYPE = "deactivate"
        const val WITHDRAW_TYPE = "withdraw"
        const val CONFIRMED_STATUS = "confirmed"
        const val FINALIZED_STATUS = "finalized"
        const val OPERATION_TYPE_NAME = "Operation"
        const val MILLIS_IN_SECOND = 1000L
    }
}