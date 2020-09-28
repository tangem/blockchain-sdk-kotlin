package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.*
import com.tangem.blockchain.network.blockchair.BlockchairApi
import com.tangem.blockchain.network.blockchair.BlockchairEthProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherApi
import com.tangem.blockchain.network.blockcypher.BlockcypherProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode


class EthereumNetworkManager(blockchain: Blockchain) {
    private val infuraPath = "v3/"

    private val api: EthereumApi by lazy {
        val baseUrl = when (blockchain) {
            Blockchain.Ethereum -> API_INFURA + infuraPath
            Blockchain.RSK -> API_RSK
            else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
        }
        createRetrofitInstance(baseUrl).create(EthereumApi::class.java)
    }

    private val blockcypherProvider: BlockcypherProvider? by lazy { //TODO: remove
        when (blockchain) {
            Blockchain.Ethereum -> {
                val blockcypherApi =
                        createRetrofitInstance(API_BLOCKCYPHER).create(BlockcypherApi::class.java)
                BlockcypherProvider(blockcypherApi, blockchain)
            }
            else -> null
        }
    }

    private val blockchairProvider: BlockchairEthProvider? by lazy {
        when (blockchain) {
            Blockchain.Ethereum -> {
                val blockchairApi =
                        createRetrofitInstance(API_BLOCKCHAIR).create(BlockchairApi::class.java)
                BlockchairEthProvider(blockchairApi)
            }
            else -> null
        }
    }

    private val apiKey = when (blockchain) {
        Blockchain.Ethereum -> INFURA_API_KEY
        Blockchain.RSK -> ""
        else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    private val provider: EthereumProvider by lazy { EthereumProvider(api, apiKey) }
    private val decimals = Blockchain.Ethereum.decimals()

    suspend fun getInfo(address: String, contractAddress: String? = null, tokenDecimals: Int? = null)
            : Result<EthereumInfoResponse> {
        return try {
            coroutineScope {
                val balanceResponse = retryIO { async { provider.getBalance(address) } }
                val txCountResponse = retryIO { async { provider.getTxCount(address) } }
                val pendingTxCountResponse = retryIO { async { provider.getPendingTxCount(address) } }

                val transactionsResponse =
                        retryIO { async { blockchairProvider?.getTransactions(address, contractAddress) } }

                var tokenBalanceResponse: Deferred<EthereumResponse>? = null
                if (contractAddress != null && tokenDecimals != null) {
                    tokenBalanceResponse = retryIO { async { provider.getTokenBalance(address, contractAddress) } }
                }

                val recentTransactions = when (val result = transactionsResponse.await()) {
                    is Result.Success -> result.data
                    else -> emptyList()
                }

                Result.Success(EthereumInfoResponse(
                        balanceResponse.await().result!!.parseAmount(decimals),
                        tokenBalanceResponse?.await()?.result?.parseAmount(tokenDecimals!!),
                        txCountResponse.await().result?.responseToNumber()?.toLong() ?: 0,
                        pendingTxCountResponse.await().result?.responseToNumber()?.toLong() ?: 0,
                        recentTransactions
                ))
            }
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val response = retryIO { provider.sendTransaction(transaction) }
            if (response.error == null) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(Exception("Code: ${(response.error.code)}, ${(response.error.message)}"))
            }
        } catch (error: Exception) {
            SimpleResult.Failure(error)
        }
    }

    suspend fun getFee(gasLimit: Long): Result<List<BigDecimal>> {
        return try {
            Result.Success(
                    provider.getGasPrice().result!!.parseFee(gasLimit)
            )
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    suspend fun getSignatureCount(address: String): Result<Int> {
        return blockcypherProvider?.getSignatureCount(address)
                ?: Result.Failure(Exception("No signature count provider found"))
    }

    private fun String.parseFee(gasLimit: Long): List<BigDecimal> {
        val gasPrice = this.responseToNumber().toBigDecimal()
        val minFee = gasPrice.multiply(gasLimit.toBigDecimal())
        val normalFee = minFee.multiply(BigDecimal(1.2)).setScale(0, RoundingMode.HALF_UP)
        val priorityFee = minFee.multiply(BigDecimal(1.5)).setScale(0, RoundingMode.HALF_UP)
        return listOf(
                minFee.movePointLeft(decimals),
                normalFee.movePointLeft(decimals),
                priorityFee.movePointLeft(decimals)
        )
    }

    private fun String.responseToNumber(): BigInteger = this.substring(2).toBigInteger(16)

    private fun String.parseAmount(decimals: Int): BigDecimal =
            this.responseToNumber().toBigDecimal().movePointLeft(decimals)

}

data class EthereumInfoResponse(
        val balance: BigDecimal,
        val tokenBalance: BigDecimal?,
        val txCount: Long,
        val pendingTxCount: Long,
        val recentTransactions: List<TransactionData>?
)

private const val INFURA_API_KEY = "613a0b14833145968b1f656240c7d245"