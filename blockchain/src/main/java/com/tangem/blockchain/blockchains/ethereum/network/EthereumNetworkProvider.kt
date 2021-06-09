package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.blockchair.BlockchairToken
import java.math.BigDecimal

interface EthereumNetworkProvider {
    suspend fun getInfo(address: String, tokens: Set<Token>): Result<EthereumInfoResponse>
    suspend fun sendTransaction(transaction: String): SimpleResult
    suspend fun getSignatureCount(address: String): Result<Int>
    suspend fun getTokensBalance(address: String, tokens: Set<Token>): Result<Map<Token, BigDecimal>>
    suspend fun findErc20Tokens(address: String): Result<List<BlockchairToken>>
    suspend fun getGasPrice(): Result<Long>
    suspend fun getGasLimit(
        to: String, from: String, data: String?,fallbackGasLimit: Long?,
    ): Result<Long>
}

data class EthereumInfoResponse(
        val coinBalance: BigDecimal,
        val tokenBalances: Map<Token, BigDecimal>,
        val txCount: Long,
        val pendingTxCount: Long,
        val recentTransactions: List<TransactionData>?
)