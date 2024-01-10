package com.tangem.blockchain.blockchains.binance.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal

interface BinanceNetworkProvider {
    val host: String
    suspend fun getInfo(address: String): Result<BinanceInfoResponse>
    suspend fun getFee(): Result<BigDecimal>
    suspend fun sendTransaction(transaction: ByteArray): SimpleResult
}

data class BinanceInfoResponse(
    val balances: Map<String, BigDecimal>,
    val accountNumber: Long?,
    val sequence: Long?,
)
