package com.tangem.blockchain.blockchains.kaspa.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

open class KaspaRestApiNetworkProvider(override val baseUrl: String) : KaspaNetworkProvider {

    private val api: KaspaApi by lazy {
        createRetrofitInstance(baseUrl).create(KaspaApi::class.java)
    }
    private val decimals = Blockchain.Kaspa.decimals()

    override suspend fun getInfo(address: String): Result<KaspaInfoResponse> {
        return try {
            coroutineScope {
                val balanceDeferred = retryIO { async { api.getBalance(address) } }
                val unspentsDeferred = retryIO { async { api.getUnspents(address) } }

                val balanceData = balanceDeferred.await()
                val unspentsData = unspentsDeferred.await()

                Result.Success(
                    KaspaInfoResponse(
                        balance = balanceData.balance!!.toBigDecimal().movePointLeft(decimals),
                        unspentOutputs = unspentsData.map {
                            KaspaUnspentOutput(
                                amount = it.utxoEntry!!.amount!!.toBigDecimal().movePointLeft(decimals),
                                outputIndex = it.outpoint!!.index!!,
                                transactionHash = it.outpoint!!.transactionId!!.hexToBytes(),
                                outputScript = it.utxoEntry!!.scriptPublicKey!!.scriptPublicKey!!.hexToBytes(),
                            )
                        },
                    ),
                )
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: KaspaTransactionBody): Result<String?> {
        return try {
            val result = retryIO { api.sendTransaction(transaction) }
            Result.Success(result.transactionId)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun calculateFee(transactionData: KaspaTransactionData): Result<KaspaFeeEstimation> {
        return try {
            coroutineScope {
                val massDataDeferred = retryIO { async { api.transactionMass(transactionData) } }
                val feeRateDataDeferred = retryIO { async { api.getFeeEstimate() } }

                val massData = massDataDeferred.await()
                val feeRateData = feeRateDataDeferred.await()

                Result.Success(
                    KaspaFeeEstimation(
                        mass = massData.mass,
                        priorityBucket = feeRateData.priorityBucket,
                        normalBuckets = feeRateData.normalBuckets,
                        lowBuckets = feeRateData.lowBuckets,
                    ),
                )
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }
}