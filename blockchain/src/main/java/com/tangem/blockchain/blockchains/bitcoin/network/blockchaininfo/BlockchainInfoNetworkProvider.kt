package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.*
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.API_BLOCKCHAIN_INFO
import com.tangem.blockchain.network.API_BLOCKCHAIN_INFO_FEE
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.*


class BlockchainInfoNetworkProvider : BitcoinNetworkProvider {
    private val api =
            createRetrofitInstance(API_BLOCKCHAIN_INFO).create(BlockchainInfoApi::class.java)
    private val feeApi =
            createRetrofitInstance(API_BLOCKCHAIN_INFO_FEE).create(BlockchainInfoFeeApi::class.java)

    private val decimals = Blockchain.Bitcoin.decimals()
    private val responseTransactionCap = 50
    override val supportsRbf = true // TODO: check

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            coroutineScope {
                val addressDeferred =
                        retryIO { async { api.getAddressData(address, null) } }
                val unspentsDeferred = retryIO { async { api.getUnspents(address) } }

                val addressData = addressDeferred.await()
                val unspents = unspentsDeferred.await()

                val transactions = addressData.transactions!!.map {
                    BasicTransactionData(
                            balanceDif = it.balanceDif!!.toBigDecimal().movePointLeft(decimals),
                            hash = it.hash!!,
                            isConfirmed = it.block != null,
                            date = Calendar.getInstance()
                                    .apply { this.timeInMillis = it.time!! * 1000 }
                    )
                }

                val unspentOutputs = unspents.unspentOutputs!!.map {
                    BitcoinUnspentOutput(
                            amount = it.amount!!.toBigDecimal().movePointLeft(decimals),
                            outputIndex = it.outputIndex!!.toLong(),
                            transactionHash = it.hash!!.hexToBytes(),
                            outputScript = it.outputScript!!.hexToBytes())
                }

                Result.Success(
                        BitcoinAddressInfo(
                                balance = addressData.finalBalance
                                        ?.toBigDecimal()?.movePointLeft(decimals)
                                        ?: 0.toBigDecimal(),
                                unspentOutputs = unspentOutputs,
                                recentTransactions = transactions
                        ))
            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return try {
            val feeData = retryIO { feeApi.getFees() }

            val minimalFeePerKb = feeData.regularFeePerByte!! * 1024
            val normalFeePerKb = feeData.regularFeePerByte * 1024 * 12 / 10 // 1.2 ratio
            val priorityFeePerKb = feeData.priorityFeePerByte!! * 1024

            Result.Success(BitcoinFee(
                    minimalFeePerKb.toBigDecimal().movePointLeft(decimals),
                    normalFeePerKb.toBigDecimal().movePointLeft(decimals),
                    priorityFeePerKb.toBigDecimal().movePointLeft(decimals)
            ))
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }


    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO { api.sendTransaction(transaction) }
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception)
        }
    }

    override suspend fun getTransaction(transactionHash: String): Result<BitcoinTransaction> {
        return try {
            val transaction = retryIO { api.getTransaction(transactionHash) }
            val inputTransactionIndexes =
                    transaction.inputs!!.map { it.prevOutput!!.transactionIndex!! }

            coroutineScope {
                val inputTransactionsDeferred = inputTransactionIndexes.associateBy(
                        { it }, { retryIO { async { api.getTransaction(it.toString(10)) } } }
                )
                val inputTransactionHashes =
                        inputTransactionsDeferred.mapValues { it.value.await().hash }

                val inputs = transaction.inputs.map {
                    BitcoinTransactionInput(
                            unspentOutput = BitcoinUnspentOutput(
                                    amount = it.prevOutput!!.value!!
                                            .toBigDecimal().movePointLeft(decimals),
                                    outputIndex = it.prevOutput.index!!.toLong(),
                                    transactionHash = inputTransactionHashes
                                            .get(it.prevOutput.transactionIndex)!!.hexToBytes(),
                                    outputScript = it.prevOutput.script!!.hexToBytes()
                            ),
                            sender = it.prevOutput.address!!,
                            sequence = it.sequence!!
                    )
                }

                val outputs = transaction.outputs!!.map {
                    BitcoinTransactionOutput(
                            amount = it.value!!.toBigDecimal().movePointLeft(decimals),
                            recipient = it.address!!
                    )
                }
                Result.Success(
                        BitcoinTransaction(
                                hash = transaction.hash!!,
                                isConfirmed = transaction.block != null,
                                time = Calendar.getInstance()
                                        .apply { timeInMillis = transaction.time!! },
                                inputs = inputs,
                                outputs = outputs
                        )
                )
            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return try {
            coroutineScope {
                val addressData = retryIO { api.getAddressData(address, null) }

                val transactions = addressData.transactions!!.toMutableList()

                if (addressData.transactions.size < addressData.transactionCount!!) {
                    when (val result = getRemainingTransactions(address, addressData.transactionCount)) {
                        is Result.Success -> transactions.addAll(result.data)
                        is Result.Failure -> return@coroutineScope Result.Failure(result.error)
                    }
                }

                var signatureCount = 0
                transactions.filter { it.balanceDif!! < 0 }
                        .forEach { signatureCount += it.inputCount!! }

                Result.Success(transactions.size)
            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    private suspend fun getRemainingTransactions(
            address: String,
            transactionsTotal: Int
    ): Result<List<BlockchainInfoTransaction>> {
        return try {
            coroutineScope {
                var transactionsRequestedCount = responseTransactionCap

                val offsetAddressDeferredList: MutableList<Deferred<BlockchainInfoAddress>> =
                        mutableListOf()

                while (transactionsRequestedCount < transactionsTotal) {
                    val offsetAddressDeferred =
                            retryIO {
                                async {
                                    api.getAddressData(address, transactionsRequestedCount)
                                }
                            }
                    offsetAddressDeferredList.add(offsetAddressDeferred)
                    transactionsRequestedCount += responseTransactionCap
                }
                val offsetAddressDataList =
                        offsetAddressDeferredList.map { it.await() }
                val transactions = offsetAddressDataList.flatMap { it.transactions!! }

                Result.Success(transactions)
            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }
}