package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal

interface EthereumNetworkProvider {
    suspend fun getInfo(address: String, tokens: Set<Token>): Result<EthereumInfoResponse>
    suspend fun sendTransaction(transaction: String): SimpleResult
    suspend fun getFee(to: String, from: String, data: String?, fallbackGasLimit: Long?): Result<EthereumFeeResponse>
    suspend fun getSignatureCount(address: String): Result<Int>
    suspend fun getTokensBalance(address: String, tokens: Set<Token>): Result<Map<Token, BigDecimal>>
}

data class EthereumInfoResponse(
        val coinBalance: BigDecimal,
        val tokenBalances: Map<Token, BigDecimal>,
        val txCount: Long,
        val pendingTxCount: Long,
        val recentTransactions: List<TransactionData>?
)

data class EthereumFeeResponse(
        val fees: List<BigDecimal>,
        val gasLimit: Long
)