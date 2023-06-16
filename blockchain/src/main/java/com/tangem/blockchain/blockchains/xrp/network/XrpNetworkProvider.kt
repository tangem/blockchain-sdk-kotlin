package com.tangem.blockchain.blockchains.xrp.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal

interface XrpNetworkProvider: NetworkProvider {
    suspend fun getInfo(address: String): Result<XrpInfoResponse>
    suspend fun sendTransaction(transaction: String): SimpleResult
    suspend fun getFee(): Result<XrpFeeResponse>
    suspend fun checkIsAccountCreated(address: String): Boolean
}

data class XrpInfoResponse(
        val balance: BigDecimal = BigDecimal.ZERO,
        val sequence: Long = 0,
        val hasUnconfirmed: Boolean = false,
        val reserveBase: BigDecimal,
        val accountFound: Boolean = true
)

data class XrpFeeResponse(
        val minimalFee: BigDecimal,
        val normalFee: BigDecimal,
        val priorityFee: BigDecimal
)