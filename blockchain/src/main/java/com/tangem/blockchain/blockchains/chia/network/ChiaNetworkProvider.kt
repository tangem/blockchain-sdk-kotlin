package com.tangem.blockchain.blockchains.chia.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal

interface ChiaNetworkProvider {
    val host: String

    suspend fun getUnspents(puzzleHash: String): Result<List<ChiaCoin>>

    suspend fun getFeeEstimate(transactionCost: Long): Result<EstimateFeeResult>

    suspend fun sendTransaction(transaction: ChiaTransactionBody): SimpleResult
}

data class EstimateFeeResult(
    val normalFee: BigDecimal,
    val priorityFee: BigDecimal
)
