package com.tangem.blockchain.blockchains.solana

import android.os.SystemClock
import com.tangem.blockchain.blockchains.solana.solanaj.core.SolanaTransaction
import com.tangem.blockchain.blockchains.solana.solanaj.model.*
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcClient
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.BlockchainSdkError.Solana
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import kotlinx.coroutines.*
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.rpc.types.SignatureStatuses
import org.p2p.solanaj.rpc.types.config.Commitment

/**
 * Created by Anton Zhilenkov on 26/01/2022.
 */
// FIXME: Refactor with wallet-core: https://tangem.atlassian.net/browse/AND-5706
internal class SolanaNetworkService(
    private val provider: SolanaRpcClient,
) : NetworkProvider {

    override val baseUrl: String = provider.baseUrl
    val endpoint: String = provider.endpoint

    suspend fun getMainAccountInfo(account: PublicKey): Result<SolanaMainAccountInfo> = withContext(Dispatchers.IO) {
        val accountInfo = getAccountInfo(account).successOr { return@withContext it }
        val tokenAccounts = accountTokensInfo(account).successOr { return@withContext it }

        val tokensByMint = tokenAccounts.map {
            SolanaTokenAccountInfo(
                value = it,
                address = it.pubkey,
                mint = it.account.data.parsed.info.mint,
                solAmount = it.account.data.parsed.info.tokenAmount.uiAmount.toBigDecimal(),
            )
        }.associateBy { it.mint }

        val txsInProgress = getTransactionsInProgressInfo(account).successOr { listOf() }
        Result.Success(
            SolanaMainAccountInfo(
                value = accountInfo,
                tokensByMint = tokensByMint,
                txsInProgress = txsInProgress,
            ),
        )
    }

    @Suppress("MagicNumber")
    private suspend fun getTransactionsInProgressInfo(account: PublicKey): Result<List<TransactionInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val allSignatures = provider.api.getSignaturesForAddress(account.toBase58(), Commitment.CONFIRMED, 20)
                val confirmedCommitmentSignatures = allSignatures
                    .filter { it.confirmationStatus == Commitment.CONFIRMED.value }

                val txInProgress = confirmedCommitmentSignatures.mapNotNull { addressSignature ->
                    provider.api.getTransaction(addressSignature.signature, Commitment.CONFIRMED)?.let { transaction ->
                        TransactionInfo(
                            addressSignature.signature,
                            transaction.meta.fee,
                            transaction.transaction.message.instructions,
                        )
                    }
                }
                Result.Success(txInProgress)
            } catch (ex: Exception) {
                Result.Failure(Solana.Api(ex))
            }
        }

    suspend fun getSignatureStatuses(signatures: List<String>): Result<SignatureStatuses> =
        withContext(Dispatchers.IO) {
            try {
                Result.Success(provider.api.getSignatureStatuses(signatures, true))
            } catch (ex: Exception) {
                Result.Failure(Solana.Api(ex))
            }
        }

    suspend fun getAccountInfoIfExist(account: PublicKey): Result<NewSolanaAccountInfo.Value> {
        return withContext(Dispatchers.IO) {
            try {
                val accountInfo = getAccountInfo(account)
                    .successOr { return@withContext it }

                if (accountInfo == null) {
                    Result.Failure(BlockchainSdkError.AccountNotFound())
                } else {
                    Result.Success(accountInfo)
                }
            } catch (ex: Exception) {
                Result.Failure(Solana.Api(ex))
            }
        }
    }

    suspend fun getTokenAccountInfoIfExist(associatedAccount: PublicKey): Result<SolanaSplAccountInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val splAccountInfo = provider.api.getSplTokenAccountInfoNew(associatedAccount)

                if (splAccountInfo.value == null) {
                    Result.Failure(BlockchainSdkError.AccountNotFound())
                } else {
                    Result.Success(SolanaSplAccountInfo(splAccountInfo.value, associatedAccount))
                }
            } catch (ex: Exception) {
                Result.Failure(Solana.Api(ex))
            }
        }
    }

    private fun getAccountInfo(account: PublicKey): Result<NewSolanaAccountInfo.Value?> {
        return try {
            val params = mapOf("commitment" to Commitment.FINALIZED)
            val accountInfo = provider.api.getAccountInfoNew(account, params)

            Result.Success(accountInfo.value)
        } catch (ex: Exception) {
            Result.Failure(Solana.Api(ex))
        }
    }

    private suspend fun accountTokensInfo(account: PublicKey): Result<List<NewSolanaTokenAccountInfo.Value>> =
        withContext(Dispatchers.IO) {
            val tokensAccountsInfoDefault = async {
                tokenAccountInfo(account, SolanaTokenProgram.ID.TOKEN.value)
            }
            val tokensAccountsInfo2022 = async {
                tokenAccountInfo(account, SolanaTokenProgram.ID.TOKEN_2022.value)
            }

            val tokensAccountsInfo = awaitAll(tokensAccountsInfoDefault, tokensAccountsInfo2022)
                .flatMap { info ->
                    info.successOr { return@withContext it }.value
                }.distinct()

            Result.Success(tokensAccountsInfo)
        }

    private fun tokenAccountInfo(account: PublicKey, programId: PublicKey): Result<NewSolanaTokenAccountInfo> {
        val params = buildMap {
            put("programId", programId)
            put("commitment", Commitment.RECENT.value)
        }

        return try {
            val result = provider.api.getTokenAccountsByOwnerNew(account, params)
            Result.Success(result)
        } catch (e: Exception) {
            Result.Failure(Solana.Api(e))
        }
    }

    suspend fun getFeeForMessage(transaction: SolanaTransaction): Result<FeeInfo> = withContext(Dispatchers.IO) {
        try {
            val params = provider.api.getFeeForMessage(transaction, Commitment.CONFIRMED)
            Result.Success(params)
        } catch (ex: Exception) {
            Result.Failure(Solana.Api(ex))
        }
    }

    suspend fun minimalBalanceForRentExemption(dataLength: Long): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val rent = provider.api.getMinimumBalanceForRentExemption(dataLength)
            Result.Success(rent)
        } catch (ex: Exception) {
            Result.Failure(Solana.Api(ex))
        }
    }

    suspend fun sendTransaction(signedTransaction: ByteArray, startSendingTimestamp: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            val elapsedTime = SystemClock.elapsedRealtime() - startSendingTimestamp

            delay(START_DELAY - elapsedTime)
            trySend(signedTransaction)
        }
    }

    private suspend fun trySend(signedTransaction: ByteArray, repeat: Int = MAX_RETRY_COUNT): Result<String> {
        return try {
            val result = provider.api.sendSignedTransaction(signedTransaction)
            Result.Success(result)
        } catch (e: Exception) {
            if (e.isBlockhashNotFound()) { // retry only if blockchain not found
                delay(RETRY_DELAY)
                if (repeat > 1) { // limit retry count
                    trySend(signedTransaction, repeat - 1)
                } else {
                    Result.Failure(Solana.Api(e))
                }
            } else {
                Result.Failure(Solana.Api(e))
            }
        }
    }

    suspend fun getRecentBlockhash(commitment: Commitment = Commitment.CONFIRMED): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Result.Success(provider.api.getRecentBlockhash(commitment))
            } catch (ex: Exception) {
                Result.Failure(Solana.Api(ex))
            }
        }
    }

    suspend fun getRecentPrioritizationFees(accounts: List<PublicKey>): Result<List<PrioritizationFee>> =
        withContext(Dispatchers.IO) {
            try {
                val fees = provider.api.getRecentPrioritizationFees(accounts)

                Result.Success(fees)
            } catch (ex: Exception) {
                Result.Failure(Solana.Api(ex))
            }
        }

    private fun Exception.isBlockhashNotFound(): Boolean {
        return message?.contains(BLOCKHASH_NOT_FOUND_ERROR) ?: false
    }

    companion object {
        // According to blockchain specifications and blockchain analytics
        const val START_DELAY = 15_000L
        const val RETRY_DELAY = 3_000L
        const val MAX_RETRY_COUNT = 5
        // Error message if blockhash not found, no code provided by RpcException
        const val BLOCKHASH_NOT_FOUND_ERROR = "Blockhash not found"
    }
}
