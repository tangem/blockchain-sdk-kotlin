package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import org.stellar.sdk.responses.FeeStatsResponse
import java.math.BigDecimal

interface StellarNetworkProvider : NetworkProvider {
    suspend fun getInfo(accountId: String): Result<StellarResponse>
    suspend fun getFeeStats(): Result<FeeStatsResponse>
    suspend fun sendTransaction(transaction: String): SimpleResult
    suspend fun checkTargetAccount(address: String, token: Token?): Result<StellarTargetAccountResponse>
    suspend fun getSignatureCount(accountId: String): Result<Int>
}

data class StellarResponse(
    val coinBalance: BigDecimal,
    val tokenBalances: Set<StellarAssetBalance>,
    val baseFee: BigDecimal,
    val baseReserve: BigDecimal,
    val sequence: Long,
    val recentTransactions: List<TransactionData>,
    val subEntryCount: Int
)

data class StellarAssetBalance(
    val balance: BigDecimal,
    val symbol: String,
    val issuer: String
)

data class StellarTargetAccountResponse(
    val accountCreated: Boolean,
    val trustlineCreated: Boolean? = null
)