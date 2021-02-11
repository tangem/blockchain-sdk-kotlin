package com.tangem.blockchain.network.blockchair

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.API_BLOCKCHAIR
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.isZero
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class BlockchairEthNetworkProvider(private val apiKey: String? = null) {

    private val api: BlockchairApi by lazy {
        val blockchainPath = "ethereum/"
        createRetrofitInstance(API_BLOCKCHAIR + blockchainPath).create(BlockchairApi::class.java)
    }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.ROOT)
    private val blockchain = Blockchain.Ethereum

    suspend fun getTransactions(address: String, tokens: Set<Token>): Result<List<TransactionData>> {
        return try {
            coroutineScope {
                val addressDeferred = retryIO {
                    async {
                        api.getAddressData(
                                address = address,
                                transactionDetails = true,
                                limit = 50,
                                key = apiKey
                        )
                    }
                }
                val tokenHolderDeferredList = tokens.map {
                    retryIO {
                        async {
                            api.getTokenHolderData(
                                    address = address,
                                    contractAddress = it.contractAddress,
                                    limit = 50,
                                    key = apiKey
                            )
                        }
                    }
                }

                val calls = addressDeferred.await().data!!
                        .getValue(address.toLowerCase(Locale.ROOT)).calls ?: emptyList()

                val tokenCalls = mutableListOf<BlockchairCallInfo>()
                tokenHolderDeferredList.forEach {
                    tokenCalls.addAll(
                            it.await().data
                                    .getValue(address.toLowerCase(Locale.ROOT)).transactions
                                    ?: emptyList())
                }

                val coinTransactions = calls.map { it.toTransactionData(tokens) }
                        .filter { !it.amount.value!!.isZero() }
                val tokenTransactions = tokenCalls.map { it.toTransactionData(tokens) }

                Result.Success(coinTransactions + tokenTransactions)
            }
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    private fun BlockchairCallInfo.toTransactionData(tokens: Set<Token>): TransactionData {
        val amount = if (contractAddress == null) { // coin transaction
            val value = BigDecimal(value).movePointLeft(blockchain.decimals())
            Amount(value, blockchain)
        } else { // token transaction
            val value = BigDecimal(value).movePointLeft(tokenDecimals!!)
            Amount(tokens.find { it.contractAddress == contractAddress }!!, value)
        }

        val status =
                if (block == -1) TransactionStatus.Unconfirmed else TransactionStatus.Confirmed
        val date = dateFormat.parse(time!!)

        return TransactionData(
                amount = amount,
                fee = null,
                sourceAddress = sender ?: "unknown",
                destinationAddress = recipient ?: "unknown",
                status = status,
                date = Calendar.getInstance().apply { time = date!! },
                hash = hash?.substring(2) // trim 0x
        )
    }
}