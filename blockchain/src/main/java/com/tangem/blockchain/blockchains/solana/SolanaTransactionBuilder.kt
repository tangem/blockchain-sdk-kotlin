package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.core.SolanaTransaction
import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedSolanaTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaComputeBudgetProgram
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.programs.AssociatedTokenProgram
import org.p2p.solanaj.programs.Program
import org.p2p.solanaj.programs.SystemProgram
import java.math.BigDecimal

internal class SolanaTransactionBuilder(
    private val account: PublicKey,
    private val multiNetworkProvider: MultiNetworkProvider<SolanaNetworkService>,
    private val tokenProgramFinder: SolanaTokenAccountInfoFinder,
) {

    suspend fun buildUnsignedTransaction(destinationAddress: String, amount: Amount): Result<SolanaTransaction> {
        val amountToSend = amount.value ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx)

        return when (amount.type) {
            is AmountType.Coin -> buildUnsignedCoinTransaction(
                destinationAddress = destinationAddress,
                amount = amountToSend,
            )
            is AmountType.Token -> buildUnsignedTokenTransaction(
                destinationAddress = destinationAddress,
                amount = amountToSend,
                token = amount.type.token,
            )
            else -> Result.Failure(BlockchainSdkError.UnsupportedOperation())
        }
    }

    fun buildUnsignedTransaction(builtTransaction: ByteArray): SolanaTransaction {
        return SolanaTransaction(account).apply {
            setSerializedMessage(builtTransaction)
        }
    }

    private suspend fun buildUnsignedCoinTransaction(
        destinationAddress: String,
        amount: BigDecimal,
    ): Result<SolanaTransaction> {
        val recentBlockHash = multiNetworkProvider.performRequest {
            getRecentBlockhash()
        }.successOr { return Result.Failure(it.error) }
        val destinationAccount = PublicKey(destinationAddress)
        val lamports = SolanaValueConverter.toLamports(amount)

        val transaction = SolanaTransaction(account).apply {
            SolanaComputeBudgetProgram.setComputeUnitPrice(
                microLamports = DEFAULT_CU_PRICE,
            ).let(::addInstruction)

            SolanaComputeBudgetProgram.setComputeUnitLimit(
                units = DEFAULT_CU_LIMIT,
            ).let(::addInstruction)

            SystemProgram.transfer(account, destinationAccount, lamports).let(::addInstruction)
            setRecentBlockHash(recentBlockHash)
        }

        return Result.Success(transaction)
    }

    private suspend fun buildUnsignedTokenTransaction(
        destinationAddress: String,
        amount: BigDecimal,
        token: Token,
    ): Result<SolanaTransaction> {
        val destinationAccount = PublicKey(destinationAddress)
        val mint = PublicKey(token.contractAddress)

        val (tokenInfo, tokenProgramId) = tokenProgramFinder.getTokenAccountInfoAndTokenProgramId(
            account = account,
            mint = mint,
        ).successOr { return it }

        val destinationAssociatedAccount = createAssociatedSolanaTokenAddress(
            account = destinationAccount,
            mint = mint,
            tokenProgramId = tokenProgramId,
        ).getOrElse {
            return Result.Failure(BlockchainSdkError.Solana.FailedToCreateAssociatedAccount)
        }

        if (tokenInfo.associatedPubK == destinationAssociatedAccount) {
            return Result.Failure(BlockchainSdkError.Solana.SameSourceAndDestinationAddress)
        }

        val transaction = SolanaTransaction(account).apply {
            addInstructions(
                tokenProgramId = tokenProgramId,
                mint = mint,
                destinationAssociatedAccount = destinationAssociatedAccount,
                destinationAccount = destinationAccount,
                sourceAssociatedAccount = tokenInfo.associatedPubK,
                token = token,
                amount = amount,
            ).successOr { return Result.Failure(it.error) }

            val recentBlockHash = multiNetworkProvider.performRequest {
                getRecentBlockhash()
            }.successOr { return it }
            setRecentBlockHash(recentBlockHash)
        }

        return Result.Success(transaction)
    }

    @Suppress("LongParameterList")
    private suspend fun SolanaTransaction.addInstructions(
        tokenProgramId: SolanaTokenProgram.ID,
        mint: PublicKey,
        destinationAssociatedAccount: PublicKey,
        destinationAccount: PublicKey,
        sourceAssociatedAccount: PublicKey,
        token: Token,
        amount: BigDecimal,
    ): SimpleResult {
        val isDestinationAccountExists = isTokenAccountExist(destinationAssociatedAccount)
            .successOr { return SimpleResult.Failure(it.error) }

        SolanaComputeBudgetProgram.setComputeUnitPrice(
            microLamports = if (isDestinationAccountExists) {
                DEFAULT_CU_PRICE
            } else {
                CREATE_NEW_ACCOUNT_CU_PRICE
            },
        ).let(::addInstruction)

        SolanaComputeBudgetProgram.setComputeUnitLimit(
            units = if (isDestinationAccountExists) {
                DEFAULT_CU_LIMIT
            } else {
                CREATE_NEW_ACCOUNT_CU_LIMIT
            },
        ).let(::addInstruction)

        if (!isDestinationAccountExists) {
            AssociatedTokenProgram.createAssociatedTokenAccountInstruction(
                /* associatedProgramId = */ Program.Id.splAssociatedTokenAccount,
                /* programId = */ tokenProgramId.value,
                /* mint = */ mint,
                /* associatedAccount = */ destinationAssociatedAccount,
                /* owner = */ destinationAccount,
                /* payer = */ account,
            ).let(::addInstruction)
        }

        SolanaTokenProgram.createTransferCheckedInstruction(
            source = sourceAssociatedAccount,
            destination = destinationAssociatedAccount,
            amount = SolanaValueConverter.toLamports(token, amount),
            owner = account,
            decimals = token.decimals.toByte(),
            tokenMint = mint,
            programId = tokenProgramId,
        ).let(::addInstruction)

        return SimpleResult.Success
    }

    private suspend fun isTokenAccountExist(associatedAccount: PublicKey): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            val infoResult = multiNetworkProvider.performRequest {
                getTokenAccountInfoIfExist(associatedAccount)
            }

            when {
                infoResult is Result.Failure && infoResult.error is BlockchainSdkError.AccountNotFound -> {
                    Result.Success(data = false)
                }
                infoResult is Result.Failure -> {
                    Result.Failure(infoResult.error)
                }
                else -> {
                    Result.Success(data = true)
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_CU_LIMIT = 200_000
        const val CREATE_NEW_ACCOUNT_CU_LIMIT = 400_000

        const val DEFAULT_CU_PRICE = 1_000_000L
        const val CREATE_NEW_ACCOUNT_CU_PRICE = 500_000L
    }
}