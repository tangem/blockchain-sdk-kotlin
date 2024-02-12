package com.tangem.blockchain.blockchains.chia.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal

interface ChiaNetworkProvider : NetworkProvider {

    suspend fun getUnspents(puzzleHash: String): Result<List<ChiaCoin>>

    suspend fun getFeeEstimate(transactionCost: Long): Result<ChiaEstimateFeeResult>

    suspend fun sendTransaction(transaction: ChiaTransactionBody): SimpleResult
}

data class ChiaEstimateFeeResult(
    val minimalFee: BigDecimal,
    val normalFee: BigDecimal,
    val priorityFee: BigDecimal,
)