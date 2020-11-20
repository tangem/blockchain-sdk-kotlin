package com.tangem.blockchain.network.blockchair

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.common.extensions.isZero
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class BlockchairEthProvider(private val api: BlockchairApi) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss")

    private val blockchain = Blockchain.Ethereum
    private val blockchainPath = "ethereum"

    suspend fun getTransactions(address: String, tokens: Set<Token>): Result<List<TransactionData>> {
        return try {
            coroutineScope {
                val addressDeferred = retryIO {
                    async {
                        api.getAddressData(
                                address = address,
                                blockchain = blockchainPath,
                                transactionDetails = true,
                                limit = 50
                        )
                    }
                }
                val tokenHolderDeferredList = tokens.map {
                    retryIO {
                        async {
                            api.getTokenHolderData(
                                    address = address,
                                    contractAddress = it.contractAddress,
                                    blockchain = blockchainPath,
                                    limit = 50
                            )
                        }
                    }
                }

                val calls = addressDeferred.await().data!!
                        .getValue(address.toLowerCase()).calls ?: emptyList()

                val tokenCalls = mutableListOf<BlockchairCallInfo>()
                tokenHolderDeferredList.forEach {
                    tokenCalls.addAll(it.await().data.getValue(address.toLowerCase()).transactions
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
            Amount(tokens.find { it.contractAddress == contractAddress}!!, value)
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