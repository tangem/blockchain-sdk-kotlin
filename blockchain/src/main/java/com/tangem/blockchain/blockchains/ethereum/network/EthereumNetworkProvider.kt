package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.blockchair.BlockchairToken
import java.math.BigDecimal
import java.math.BigInteger

interface EthereumNetworkProvider : NetworkProvider {
    suspend fun getInfo(address: String, tokens: Set<Token>): Result<EthereumInfoResponse>
    suspend fun getPendingTxCount(address: String): Result<Long>
    suspend fun getAllowance(ownerAddress: String, token: Token, spenderAddress: String): kotlin.Result<BigDecimal>
    suspend fun sendTransaction(transaction: String): SimpleResult
    suspend fun getSignatureCount(address: String): Result<Int>
    suspend fun findErc20Tokens(address: String): Result<List<BlockchairToken>>
    suspend fun getGasPrice(): Result<BigInteger>
    suspend fun getGasLimit(to: String, from: String, value: String?, data: String?): Result<BigInteger>
    suspend fun getFeeHistory(): Result<EthereumFeeHistory>
    suspend fun getTokensBalance(address: String, tokens: Set<Token>): Result<Map<Token, BigDecimal>>

    suspend fun callContractForFee(data: ContractCallData): Result<BigInteger>
}

class EthereumInfoResponse(
    val coinBalance: BigDecimal,
    val tokenBalances: Map<Token, BigDecimal>,
    val txCount: Long,
    val pendingTxCount: Long,
    val recentTransactions: List<TransactionData.Uncompiled>?,
)

sealed interface EthereumFeeHistory {

    val baseFee: BigDecimal

    data class Common(
        override val baseFee: BigDecimal,
        val lowPriorityFee: BigDecimal,
        val marketPriorityFee: BigDecimal,
        val fastPriorityFee: BigDecimal,
    ) : EthereumFeeHistory {

        fun toTriple() = Triple(lowPriorityFee, marketPriorityFee, fastPriorityFee)
    }

    data class Fallback(val gasPrice: BigInteger) : EthereumFeeHistory {
        override val baseFee: BigDecimal = BigDecimal.ZERO
    }
}
