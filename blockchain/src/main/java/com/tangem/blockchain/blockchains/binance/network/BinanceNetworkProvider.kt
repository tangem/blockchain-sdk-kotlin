package com.tangem.blockchain.blockchains.binance.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

interface BinanceNetworkProvider : NetworkProvider {
    suspend fun getInfo(address: String): Result<BinanceInfoResponse>
    suspend fun getFee(): Result<BigDecimal>
    suspend fun sendTransaction(transaction: ByteArray): Result<String>
}

data class BinanceInfoResponse(
    val balances: Map<String, BigDecimal>,
    val accountNumber: Long?,
    val sequence: Long?,
)