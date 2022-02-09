package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.core.Transaction
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.RpcClient
import com.tangem.blockchain.extensions.Result
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.programs.Program
import org.p2p.solanaj.rpc.Cluster
import org.p2p.solanaj.rpc.RpcException
import org.p2p.solanaj.rpc.types.FeesInfo
import org.p2p.solanaj.rpc.types.SignatureStatuses
import org.p2p.solanaj.rpc.types.TokenAccountInfo
import org.p2p.solanaj.rpc.types.TransactionResult
import org.p2p.solanaj.rpc.types.config.Commitment
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class SolanaNetworkService(
    private val provider: RpcClient
) {

    suspend fun getInfo(account: PublicKey): Result<SolanaAccountInfo> {
        val mainAccountResult = accountInfo(account)
        val mainAccount: SolanaMainAccountInfo = (mainAccountResult as? Result.Success)?.data
            ?: return mainAccountResult as Result.Failure

        val tokenAccountsResult = tokenAccountsInfo(account)
        val tokenAccounts: List<TokenAccountInfo.Value> = (tokenAccountsResult as? Result.Success)?.data
            ?: return tokenAccountsResult as Result.Failure

        val tokensByMint = tokenAccounts.map {
            SolanaTokenAccountInfo(
                address = it.pubkey,
                mint = it.account.data.parsed.info.mint,
                uiAmount = it.account.data.parsed.info.tokenAmount.uiAmount.toBigDecimal(),
            )
        }.associateBy { it.mint }

        val transactionsResult = getInProgressTransactionsInfo(account)
        val txsInProgress: List<TransactionInfo> = (transactionsResult as? Result.Success)?.data
            ?: listOf()

        return Result.Success(SolanaAccountInfo(
            balance = mainAccount.balance,
            accountExists = mainAccount.accountExists,
            tokensByMint = tokensByMint,
            txsInProgress = txsInProgress
        ))
    }

    private fun getInProgressTransactionsInfo(account: PublicKey): Result<List<TransactionInfo>> {
        return try {
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
        } catch (ex: RpcException) {
            Result.Failure(ex)
        }
    }

    fun getSignatureStatuses(signatures: List<String>): Result<SignatureStatuses> {
        return try {
            Result.Success(provider.api.getSignatureStatuses(signatures, true))
        } catch (ex: RpcException) {
            Result.Failure(ex)
        }
    }

    fun accountInfo(account: PublicKey): Result<SolanaMainAccountInfo> {
        return try {
            val accountInfo = provider.api.getAccountInfo(account, Commitment.FINALIZED.toMap())
            return if (accountInfo.value == null) {
                Result.Success(SolanaMainAccountInfo(0L, false))
            } else {
                Result.Success(SolanaMainAccountInfo(accountInfo.value.lamports, true))
            }
        } catch (ex: RpcException) {
            Result.Failure(ex)
        }
    }

    fun tokenAccountsInfo(account: PublicKey): Result<List<TokenAccountInfo.Value>> {
        return try {
            val params = mutableMapOf<String, Any>("programId" to Program.Id.token)
            params.addCommitment(Commitment.RECENT)
            val tokensAccountsInfo = provider.api.getTokenAccountsByOwner(account, params, mutableMapOf())
            Result.Success(tokensAccountsInfo.value)
        } catch (ex: RpcException) {
            Result.Failure(ex)
        }
    }

    fun isAccountExist(account: PublicKey): Result<Boolean> {
        return when (val result = accountInfo(account)) {
            is Result.Success -> Result.Success(result.data.accountExists)
            is Result.Failure -> result
        }
    }

    fun getFees(): Result<FeesInfo> {
        return try {
            val params = provider.api.getFees(Commitment.FINALIZED)
            Result.Success(params)
        } catch (ex: RpcException) {
            Result.Failure(ex)
        }
    }

    fun mainAccountCreationFee(): BigDecimal = accountRentFeeByEpoch()

    fun accountRentFeeByEpoch(numberOfEpochs: Int = 1): BigDecimal {
        // https://docs.solana.com/developing/programming-model/accounts#calculation-of-rent
        // result in lamports
        val minimumAccountSizeInBytes = BigDecimal(MIN_ACCOUNT_SIZE)

        val rentInLamportPerByteEpoch = BigDecimal(determineRentPerByteEpoch(provider.cluster))
        val rentFeePerEpoch = minimumAccountSizeInBytes
            .multiply(numberOfEpochs.toBigDecimal())
            .multiply(rentInLamportPerByteEpoch)

        return rentFeePerEpoch
    }

    fun tokenAccountCreationFee(): Result<BigDecimal> = minimalBalanceForRentExemption()

    fun minimalBalanceForRentExemption(): Result<BigDecimal> {
        return try {
            val rent = provider.api.getMinimumBalanceForRentExemption(0)
            Result.Success(rent.toBigDecimal())
        } catch (ex: RpcException) {
            Result.Failure(ex)
        }
    }

    fun sendTransaction(signedTransaction: Transaction): Result<String> {
        return try {
            val result = provider.api.sendSignedTransaction(signedTransaction)
            Result.Success(result)
        } catch (ex: RpcException) {
            Result.Failure(ex)
        }
    }

    fun getRecentBlockhash(commitment: Commitment? = null): String = provider.api.getRecentBlockhash(commitment)

    private fun determineRentPerByteEpoch(cluster: Cluster): Double {
        return when (cluster) {
            Cluster.MAINNET -> RENT_PER_BYTE_EPOCH
            Cluster.TESTNET -> RENT_PER_BYTE_EPOCH
            Cluster.DEVNET -> RENT_PER_BYTE_EPOCH_DEV_NET
        }
    }

    companion object {
        const val MIN_ACCOUNT_SIZE = 128L
        const val RENT_PER_BYTE_EPOCH = 19.055441478439427
        const val RENT_PER_BYTE_EPOCH_DEV_NET = 0.359375
    }
}


data class SolanaMainAccountInfo(
    val balance: Long,
    val accountExists: Boolean,
)

data class SolanaAccountInfo(
    val balance: Long,
    val accountExists: Boolean,
    val tokensByMint: Map<String, SolanaTokenAccountInfo>,
    val txsInProgress: List<TransactionInfo>
)

data class SolanaTokenAccountInfo(
    val address: String,
    val mint: String,
    val uiAmount: BigDecimal, // in SOL
)

data class TransactionInfo(
    val signature: String,
    val fee: Long, // in lamports
    val instructions: List<TransactionResult.Instruction>
)

private fun MutableMap<String, Any>.addCommitment(commitment: Commitment): MutableMap<String, Any> {
    this["commitment"] = commitment
    return this
}

private fun Commitment.toMap(): MutableMap<String, Any> {
    return mutableMapOf("commitment" to this)
}