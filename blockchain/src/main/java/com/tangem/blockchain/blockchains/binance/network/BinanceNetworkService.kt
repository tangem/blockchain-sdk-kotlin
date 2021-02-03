package com.tangem.blockchain.blockchains.binance.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal

interface BinanceNetworkService {
    suspend fun getInfo(address: String): Result<BinanceInfoResponse>
    suspend fun getFee(): Result<BigDecimal>
    suspend fun sendTransaction(transaction: ByteArray): SimpleResult
}