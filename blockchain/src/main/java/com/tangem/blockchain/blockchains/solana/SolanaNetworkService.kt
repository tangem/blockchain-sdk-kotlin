package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.core.SolanaTransaction
import com.tangem.blockchain.blockchains.solana.solanaj.model.*
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgramId
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcClient
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.BlockchainSdkError.Solana
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
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

    override val baseUrl: String = provider.host
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
                    Result.Failure(BlockchainSdkError.AccountNotFound)
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
                val splAccountInfo = provider.api.getSplTokenAccountInfo(associatedAccount)

                if (splAccountInfo.value == null) {
                    Result.Failure(BlockchainSdkError.AccountNotFound)
                } else {
                    Result.Success(SolanaSplAccountInfo(splAccountInfo.value, associatedAccount))
                }
            } catch (ex: Exception) {
                Result.Failure(Solana.Api(ex))
            }
        }
    }

    private suspend fun getAccountInfo(account: PublicKey): Result<NewSolanaAccountInfo.Value?> {
        return withContext(Dispatchers.IO) {
            try {
                val params = mapOf("commitment" to Commitment.FINALIZED)
                val accountInfo = provider.api.getAccountInfoNew(account, params)

                Result.Success(accountInfo.value)
            } catch (ex: Exception) {
                Result.Failure(Solana.Api(ex))
            }
        }
    }

    private suspend fun accountTokensInfo(account: PublicKey): Result<List<NewSolanaTokenAccountInfo.Value>> =
        withContext(Dispatchers.IO) {
            try {
                val tokensAccountsInfoDefault = async {
                    tokenAccountInfo(account, SolanaTokenProgramId.TOKEN.value)
                }
                val tokensAccountsInfo2022 = async {
                    tokenAccountInfo(account, SolanaTokenProgramId.TOKEN_2022.value)
                }

                val tokensAccountsInfo = awaitAll(tokensAccountsInfoDefault, tokensAccountsInfo2022)
                    .flatMap { it.value }
                    .distinct()

                Result.Success(tokensAccountsInfo)
            } catch (ex: Exception) {
                Result.Failure(Solana.Api(ex))
            }
        }

    private fun tokenAccountInfo(account: PublicKey, programId: PublicKey): NewSolanaTokenAccountInfo {
        val params = buildMap {
            put("programId", programId)
            put("commitment", Commitment.RECENT.value)
        }

        return provider.api.getTokenAccountsByOwnerNew(account, params)
    }

    suspend fun getFeeForMessage(transaction: SolanaTransaction): Result<FeeInfo> = withContext(Dispatchers.IO) {
        try {
            val params = provider.api.getFeeForMessage(transaction, Commitment.PROCESSED)
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

    suspend fun sendTransaction(signedTransaction: SolanaTransaction): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = provider.api.sendSignedTransaction(signedTransaction)
            Result.Success(result)
        } catch (ex: Exception) {
            Result.Failure(Solana.Api(ex))
        }
    }

    suspend fun getRecentBlockhash(commitment: Commitment? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            Result.Success(provider.api.getRecentBlockhash(commitment))
        } catch (ex: Exception) {
            Result.Failure(Solana.Api(ex))
        }
    }
}
