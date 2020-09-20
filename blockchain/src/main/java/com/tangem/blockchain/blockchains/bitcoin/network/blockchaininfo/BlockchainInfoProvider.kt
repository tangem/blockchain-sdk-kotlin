package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinProvider
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.*


class BlockchainInfoProvider(
        private val blockchainApi: BlockchainInfoApi,
        private val bitcoinfeesEarnApi: BitcoinfeesEarnApi
) : BitcoinProvider {
    private val decimals = Blockchain.Bitcoin.decimals()
    private val responseTransactionCap = 50

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            coroutineScope {
                val addressDeferred = retryIO { async { blockchainApi.getAddressData(address, null) } }
                val unspentsDeferred = retryIO { async { blockchainApi.getUnspents(address) } }

                val addressData = addressDeferred.await()
                val unspents = unspentsDeferred.await()

                val transactions = addressData.transactions!!.map {
                    BasicTransactionData(
                            balanceDif = it.balanceDif!!.toBigDecimal().movePointLeft(decimals),
                            hash = it.hash!!,
                            isConfirmed = it.blockHeight != 0L,
                            date = Calendar.getInstance().apply { this.timeInMillis = it.time!! * 1000 }
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
                                balance = addressData.finalBalance?.toBigDecimal()?.movePointLeft(decimals)
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
            val feeData = retryIO { bitcoinfeesEarnApi.getFees() }

            val minimalFeePerKb = feeData.minimalFeePerByte!! * 1024
            val normalFeePerKb = feeData.normalFeePerByte!! * 1024
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
            retryIO { blockchainApi.sendTransaction(transaction) }
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception)
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return try {
            coroutineScope {
                val addressData = retryIO { blockchainApi.getAddressData(address, null) }

                val transactions = addressData.transactions!!.toMutableList()

                if (addressData.transactions.size < addressData.transactionCount!!) {
                    when (val result = getRemainingTransactions(address, addressData.transactionCount)) {
                        is Result.Success -> transactions.addAll(result.data)
                        is Result.Failure -> return@coroutineScope Result.Failure(result.error)
                    }
                }

                var signatureCount = 0
                transactions.filter { it.balanceDif!! < 0 }.forEach { signatureCount += it.inputCount!! }

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
                                    blockchainApi.getAddressData(address, transactionsRequestedCount)
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