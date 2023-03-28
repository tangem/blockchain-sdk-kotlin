package com.tangem.blockchain.blockchains.ergo.network.api

import com.tangem.blockchain.blockchains.ergo.network.ErgoAddressResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoAddressRequestData
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiBlockResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiSendTransactionResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiTransactionRespone
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiUnspentResponse
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ErgoApiNetworkProvider(baseUrl: String) : ErgoNetworkProvider {
    override val host: String = baseUrl
    private val api: ErgoApi by lazy {
        createRetrofitInstance(baseUrl).create(ErgoApi::class.java)
    }
//https://ergo-explorer.getblok.io/api/v0
    override suspend fun getInfo(data: ErgoAddressRequestData): Result<ErgoAddressResponse> {
        return try {
            coroutineScope {
                val addressDeferred = retryIO { async { api.getAddressData(data.address) } }
                val addressesData = addressDeferred.await()
                val confirmedTransactionsDeferred = data.recentTransactions.map {
                    retryIO {
                        async {
                            checkIfTransactionConfirmed(it)
                        }
                    }
                }
                Result.Success(
                    ErgoAddressResponse(
                        addressesData.balance!!,
                        confirmedTransactionsDeferred.awaitAll().filter { it.summary!!.confirmationsCount!! > 0 }
                            .mapNotNull {
                                it
                                    .summary!!.id
                            },
                    ),
                )
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private suspend fun checkIfTransactionConfirmed(transaction: String): ErgoApiTransactionRespone {
        return api.getTransaction(transaction)
    }

    override suspend fun getLastBlock(): Result<ErgoApiBlockResponse> {
        return try {
            coroutineScope {
                val blockDeferred = async { api.getLastBlock() }
                val blockData = blockDeferred.await()
                Result.Success(blockData)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getUnspent(address: String): Result<List<ErgoApiUnspentResponse>> {
        return try {
            coroutineScope {
                val unspentDeferred = async { api.getUnspent(address) }
                val unspentData = unspentDeferred.await()
                Result.Success(unspentData)
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: String): Result<ErgoApiSendTransactionResponse> {
        return try {
            val txId = retryIO { api.sendTransaction(transaction) }
            Result.Success(txId)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }
}
