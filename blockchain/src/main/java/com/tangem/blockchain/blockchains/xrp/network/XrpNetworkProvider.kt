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
    suspend fun checkTargetAccount(address: String, token: Token?): Result<XrpTargetAccountResponse>
    suspend fun checkDestinationTagRequired(address: String): Boolean
}

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