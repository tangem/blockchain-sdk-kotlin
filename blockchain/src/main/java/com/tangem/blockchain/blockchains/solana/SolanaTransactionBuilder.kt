package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.core.SolanaTransaction
import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedSolanaTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgramId
import com.tangem.blockchain.blockchains.solana.solanaj.program.createSolanaTransferCheckedInstruction
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
        return when (amount.type) {
            is AmountType.Coin -> buildUnsignedCoinTransaction(
                destinationAddress = destinationAddress,
                amount = amount,
            )
            is AmountType.Token -> buildUnsignedTokenTransaction(
                destinationAddress = destinationAddress,
                amount = amount,
                token = amount.type.token,
            )
            is AmountType.Reserve -> Result.Failure(BlockchainSdkError.UnsupportedOperation())
        }
    }

    private suspend fun buildUnsignedCoinTransaction(
        destinationAddress: String,
        amount: Amount,
    ): Result<SolanaTransaction> {
        val recentBlockHash = multiNetworkProvider.performRequest { getRecentBlockhash() }.successOr {
            return Result.Failure(it.error)
        }
        val destinationAccount = PublicKey(destinationAddress)
        val lamports = SolanaValueConverter.toLamports(amount.value ?: BigDecimal.ZERO)

        val transaction = SolanaTransaction(account)
        transaction.addInstruction(SystemProgram.transfer(account, destinationAccount, lamports))
        transaction.setRecentBlockHash(recentBlockHash)

        return Result.Success(transaction)
    }

    private suspend fun buildUnsignedTokenTransaction(
        destinationAddress: String,
        amount: Amount,
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
                amount = amount.value,
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
        tokenProgramId: SolanaTokenProgramId,
        mint: PublicKey,
        destinationAssociatedAccount: PublicKey,
        destinationAccount: PublicKey,
        sourceAssociatedAccount: PublicKey,
        token: Token,
        amount: BigDecimal?,
    ): SimpleResult {
        val isDestinationAccountExists = isTokenAccountExist(destinationAssociatedAccount)
            .successOr { return SimpleResult.Failure(it.error) }

        if (!isDestinationAccountExists) {
            val associatedTokenInstruction = AssociatedTokenProgram.createAssociatedTokenAccountInstruction(
                /* associatedProgramId = */ Program.Id.splAssociatedTokenAccount,
                /* programId = */ tokenProgramId.value,
                /* mint = */ mint,
                /* associatedAccount = */ destinationAssociatedAccount,
                /* owner = */ destinationAccount,
                /* payer = */ account,
            )
            addInstruction(associatedTokenInstruction)
        }

        val sendInstruction = createSolanaTransferCheckedInstruction(
            source = sourceAssociatedAccount,
            destination = destinationAssociatedAccount,
            amount = SolanaValueConverter.toLamports(
                token = token,
                value = amount ?: BigDecimal.ZERO,
            ),
            owner = account,
            decimals = token.decimals.toByte(),
            tokenMint = mint,
            programId = tokenProgramId,
        )
        addInstruction(sendInstruction)

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
}
