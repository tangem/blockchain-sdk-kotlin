package com.tangem.blockchain.network.blockchair

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.isApiKeyNeeded
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.API_BLOCKCHAIR
import com.tangem.blockchain.network.API_BLOCKCKAIR_TANGEM
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.isZero
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BlockchairEthNetworkProvider(
    private val apiKey: String? = null,
    private val authorizationToken: String? = null,
) {

    private val api: BlockchairApi by lazy {
        val api = if (apiKey != null) API_BLOCKCHAIR else API_BLOCKCKAIR_TANGEM
        val host = api + ETHEREUM_BLOCKCHAIN_PATH
        createRetrofitInstance(host).create(BlockchairApi::class.java)
    }

    private var currentApiKey: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.ROOT)
    private val blockchain = Blockchain.Ethereum

    suspend fun getTransactions(address: String, tokens: Set<Token>): Result<List<TransactionData>> {
        return try {
            coroutineScope {
                val addressDeferred = makeRequestUsingKeyOnlyWhenNeeded {
                    async {
                        api.getAddressData(
                            address = address,
                            transactionDetails = true,
                            limit = 50,
                            key = apiKey,
                            authorizationToken = authorizationToken,
                        )
                    }
                }
                val tokenHolderDeferredList = tokens.map {
                    makeRequestUsingKeyOnlyWhenNeeded {
                        async {
                            api.getTokenHolderData(
                                address = address,
                                contractAddress = it.contractAddress,
                                limit = 50,
                                key = apiKey,
                                authorizationToken = authorizationToken,
                            )
                        }
                    }
                }

                val calls = addressDeferred.await().data!!
                    .getValue(address.lowercase(Locale.ROOT)).calls ?: emptyList()

                val tokenCalls = mutableListOf<BlockchairCallInfo>()
                tokenHolderDeferredList.forEach {
                    tokenCalls.addAll(
                        it.await().data
                            .getValue(address.lowercase(Locale.ROOT)).transactions
                            ?: emptyList(),
                    )
                }

                val coinTransactions = calls.map { it.toTransactionData(tokens) }
                    .filter { !it.amount.value!!.isZero() }
                val tokenTransactions = tokenCalls.map { it.toTransactionData(tokens) }

                Result.Success(coinTransactions + tokenTransactions)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
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
            hash = hash?.substring(2), // trim 0x
        )
    }

    suspend fun findErc20Tokens(address: String): Result<List<BlockchairToken>> {
        return try {
            val tokens = makeRequestUsingKeyOnlyWhenNeeded {
                api.findErc20Tokens(
                    address = address,
                    key = apiKey,
                    authorizationToken = authorizationToken,
                )
            }.data
                ?.getValue(address.lowercase(Locale.ROOT))?.tokensInfo?.tokens
                ?: emptyList()
            Result.Success(tokens)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private suspend fun <T> makeRequestUsingKeyOnlyWhenNeeded(block: suspend () -> T): T {
        return try {
            retryIO { block() }
        } catch (error: HttpException) {
            if (error.isApiKeyNeeded(currentApiKey, apiKey)) {
                currentApiKey = apiKey
                retryIO { block() }
            } else {
                throw error
            }
        }
    }

    companion object {
        private const val ETHEREUM_BLOCKCHAIN_PATH = "ethereum/"
    }
}
