package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.core.Transaction
import com.tangem.blockchain.blockchains.solana.solanaj.program.TokenProgramId
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.RpcClient
import com.tangem.blockchain.common.BlockchainSdkError.Solana
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.rpc.Cluster
import org.p2p.solanaj.rpc.RpcException
import org.p2p.solanaj.rpc.types.*
import org.p2p.solanaj.rpc.types.config.Commitment
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
// FIXME: Refactor with wallet-core: [REDACTED_JIRA]
class SolanaNetworkService(
    private val provider: RpcClient,
) : NetworkProvider {

    override val baseUrl: String = provider.host

    suspend fun getMainAccountInfo(account: PublicKey): Result<SolanaMainAccountInfo> = withContext(Dispatchers.IO) {
        val accountInfo = accountInfo(account).successOr { return@withContext it }
        val tokenAccounts = accountTokensInfo(account).successOr { return@withContext it }

        val tokensByMint = tokenAccounts.map {
            SolanaTokenAccountInfo(
                value = it,
                address = it.pubkey,
                mint = it.account.data.parsed.info.mint,
                uiAmount = it.account.data.parsed.info.tokenAmount.uiAmount.toBigDecimal(),
            )
        }.associateBy { it.mint }

        val txsInProgress = getTransactionsInProgressInfo(account).successOr { listOf() }
        Result.Success(
            SolanaMainAccountInfo(
                value = accountInfo.value,
                tokensByMint = tokensByMint,
                txsInProgress = txsInProgress,
            ),
        )
    }

    @Suppress("MagicNumber")
    private suspend fun getTransactionsInProgressInfo(
        account: PublicKey,
    ): Result<List<TransactionInfo>> = withContext(Dispatchers.IO) {
        try {
            val allSignatures = provider.api.getSignaturesForAddress(account.toBase58(), Commitment.CONFIRMED, 20)
            val confirmedCommitmentSignatures = allSignatures
                .filter { it.confirmationStatus == Commitment.CONFIRMED.value }

            val txInProgress = confirmedCommitmentSignatures.mapNotNull { addressSignature ->
                provider.api.getTransaction(addressSignature.signature, Commitment.CONFIRMED)?.let { transaction ->
                    TransactionInfo(
                        addressSignature.signature,
                        transaction.meta.fee,
                        transaction.transaction.message.instructions
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

    private suspend fun accountInfo(account: PublicKey): Result<AccountInfo> = withContext(Dispatchers.IO) {
        try {
            Result.Success(provider.api.getAccountInfo(account, Commitment.FINALIZED.toMap()))
        } catch (ex: Exception) {
            Result.Failure(Solana.Api(ex))
        }
    }

    private suspend fun accountTokensInfo(
        account: PublicKey,
    ): Result<List<TokenAccountInfo.Value>> = withContext(Dispatchers.IO) {
        try {
            val tokensAccountsInfoDefault = async {
                tokenAccountInfo(account, TokenProgramId.TOKEN.value)
            }
            val tokensAccountsInfo2022 = async {
                tokenAccountInfo(account, TokenProgramId.TOKEN_2022.value)
            }

            val tokensAccountsInfo = awaitAll(tokensAccountsInfoDefault, tokensAccountsInfo2022)
                .flatMap { it.value }
                .distinct()

            Result.Success(tokensAccountsInfo)
        } catch (ex: Exception) {
            Result.Failure(Solana.Api(ex))
        }
    }

    private fun tokenAccountInfo(account: PublicKey, programId: PublicKey): TokenAccountInfo {
        val params = mutableMapOf<String, Any>("programId" to programId)
            .apply { addCommitment(Commitment.RECENT) }

        return provider.api.getTokenAccountsByOwner(account, params, mutableMapOf())
    }

    private suspend fun splAccountInfo(
        associatedAccount: PublicKey,
    ): Result<SolanaSplAccountInfo> = withContext(Dispatchers.IO) {
        try {
            val splAccountInfo = provider.api.getSplTokenAccountInfo(associatedAccount)
            Result.Success(SolanaSplAccountInfo(splAccountInfo.value, associatedAccount))
        } catch (ex: Exception) {
            Result.Failure(Solana.Api(ex))
        }
    }

    suspend fun getFees(): Result<FeesInfo> = withContext(Dispatchers.IO) {
        try {
            val params = provider.api.getFees(Commitment.FINALIZED)
            Result.Success(params)
        } catch (ex: Exception) {
            Result.Failure(Solana.Api(ex))
        }
    }

    suspend fun isAccountExist(account: PublicKey): Result<Boolean> = withContext(Dispatchers.IO) {
        val info = accountInfo(account).successOr { return@withContext it }
        Result.Success(info.accountExist)
    }

    suspend fun isTokenAccountExist(associatedAccount: PublicKey): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            val info = splAccountInfo(associatedAccount).successOr { return@withContext it }

            Result.Success(info.accountExist)
        }
        }

    fun mainAccountCreationFee(): BigDecimal = accountRentFeeByEpoch(1)

    suspend fun tokenAccountCreationFee(): Result<BigDecimal> = minimalBalanceForRentExemption(BUFFER_LENGTH)

    internal fun accountRentFeeByEpoch(numberOfEpochs: Int = 1): BigDecimal {
        // https://docs.solana.com/developing/programming-model/accounts#calculation-of-rent
        // result in lamports
        val minimumAccountSizeInBytes = BigDecimal(MIN_ACCOUNT_SIZE)

        val rentInLamportPerByteEpoch = BigDecimal(determineRentPerByteEpoch(provider.endpoint))
        val rentFeePerEpoch = minimumAccountSizeInBytes
            .multiply(numberOfEpochs.toBigDecimal())
            .multiply(rentInLamportPerByteEpoch)

        return rentFeePerEpoch
    }

    suspend fun minimalBalanceForRentExemption(dataLength: Long = 0): Result<BigDecimal> = withContext(Dispatchers.IO) {
        try {
            val rent = provider.api.getMinimumBalanceForRentExemption(dataLength)
            Result.Success(rent.toBigDecimal())
        } catch (ex: Exception) {
            Result.Failure(Solana.Api(ex))
        }
    }

    suspend fun sendTransaction(signedTransaction: Transaction): Result<String> = withContext(Dispatchers.IO) {
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

    private fun determineRentPerByteEpoch(endpoint: String): Double = when (endpoint) {
        Cluster.TESTNET.endpoint -> RENT_PER_BYTE_EPOCH
        Cluster.DEVNET.endpoint -> RENT_PER_BYTE_EPOCH_DEV_NET
        else -> RENT_PER_BYTE_EPOCH
    }

    companion object {
        const val MIN_ACCOUNT_SIZE = 128L
        const val RENT_PER_BYTE_EPOCH = 19.055441478439427
        const val RENT_PER_BYTE_EPOCH_DEV_NET = 0.359375
        const val BUFFER_LENGTH = 165L
    }
}

private val AccountInfo.accountExist
    get() = value != null

data class SolanaMainAccountInfo(
    val value: AccountInfo.Value?,
    val tokensByMint: Map<String, SolanaTokenAccountInfo>,
    val txsInProgress: List<TransactionInfo>,
) {
    val balance: Long
        get() = value?.lamports ?: 0L

    val accountExist: Boolean
        get() = value != null

    val requireValue: AccountInfo.Value
        get() = value!!
}

data class SolanaSplAccountInfo(
    val value: TokenResultObjects.Value?,
    val associatedPubK: PublicKey,
) {
    val accountExist: Boolean
        get() = value != null

    val requireValue: TokenResultObjects.Value
        get() = value!!
}

data class SolanaTokenAccountInfo(
    val value: TokenAccountInfo.Value,
    val address: String,
    val mint: String,
    val uiAmount: BigDecimal, // in SOL
)

data class TransactionInfo(
    val signature: String,
    val fee: Long, // in lamports
    val instructions: List<TransactionResult.Instruction>,
)

private fun MutableMap<String, Any>.addCommitment(commitment: Commitment): MutableMap<String, Any> {
    this["commitment"] = commitment
    return this
}

private fun Commitment.toMap(): MutableMap<String, Any> {
    return mutableMapOf("commitment" to this)
}