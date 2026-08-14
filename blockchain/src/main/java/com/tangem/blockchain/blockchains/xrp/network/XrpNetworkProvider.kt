package com.tangem.blockchain.blockchains.xrp.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal

interface XrpNetworkProvider : NetworkProvider {
    suspend fun getInfo(address: String): Result<XrpInfoResponse>
    suspend fun sendTransaction(transaction: String): SimpleResult
    suspend fun getFee(): Result<XrpFeeResponse>
    suspend fun checkIsAccountCreated(address: String): Boolean
    suspend fun hasTransferRate(address: String): Boolean
    suspend fun checkTargetAccount(address: String, token: Token?): Result<XrpTargetAccountResponse>
    suspend fun getSequence(address: String): Result<Long>
    suspend fun checkDestinationTagRequired(address: String): Boolean
    suspend fun getAccountTransactions(request: XrpAccountTxRequest): Result<XrpAccountTxResponse>
}

/**
 * Request of the account transaction history page.
 *
 * @property address account address
 * @property limit   maximum number of transactions in the page
 * @property marker  position to continue from, `null` for the first page
 */
data class XrpAccountTxRequest(
    val address: String,
    val limit: Int,
    val marker: XrpTransactionMarker? = null,
)

/**
 * Account transaction history page.
 *
 * @property transactions transactions of the page
 * @property marker       position to continue from, `null` if the last page has been reached
 */
data class XrpAccountTxResponse(
    val transactions: List<XrpTransaction>,
    val marker: XrpTransactionMarker? = null,
)

data class XrpTransactionMarker(val ledger: Long, val seq: Long)

/**
 * Transaction of the account history.
 *
 * @property amount            transferred amount, absent for transaction types that don't move funds
 * @property limitAmount       trust line limit, present only for the `TrustSet` transaction type
 * @property feeInDrops        fee in drops, i.e. not scaled by the blockchain decimals
 * @property date              seconds since the Ripple Epoch
 * @property transactionResult engine result code of the transaction, `tesSUCCESS` if it succeeded
 */
data class XrpTransaction(
    val hash: String,
    val account: String,
    val destination: String?,
    val amount: XrpTransactionAmount?,
    val limitAmount: XrpIssuedCurrencyAmount?,
    val feeInDrops: BigDecimal?,
    val transactionType: String?,
    val date: Long?,
    val isValidated: Boolean,
    val transactionResult: String?,
)

sealed interface XrpTransactionAmount {

    /** Native XRP amount in drops, i.e. not scaled by the blockchain decimals */
    data class Drops(val value: BigDecimal) : XrpTransactionAmount

    data class IssuedCurrency(val amount: XrpIssuedCurrencyAmount) : XrpTransactionAmount
}

data class XrpIssuedCurrencyAmount(
    val currency: String,
    val issuer: String,
    val value: BigDecimal,
)

data class XrpInfoResponse(
    val balance: BigDecimal = BigDecimal.ZERO,
    val sequence: Long = 0,
    val hasUnconfirmed: Boolean = false,
    val reserveBase: BigDecimal,
    val reserveTotal: BigDecimal,
    val reserveInc: BigDecimal,
    val accountFound: Boolean = true,
    val tokenBalances: Set<XrpTokenBalance>,
)

data class XrpTokenBalance(
    val balance: BigDecimal,
    val issuer: String,
    val currency: String,
    val noRipple: Boolean = false,
)

data class XrpTargetAccountResponse(
    val accountCreated: Boolean,
    val trustlineCreated: Boolean? = null,
)

data class XrpFeeResponse(
    val minimalFee: BigDecimal,
    val normalFee: BigDecimal,
    val priorityFee: BigDecimal,
)