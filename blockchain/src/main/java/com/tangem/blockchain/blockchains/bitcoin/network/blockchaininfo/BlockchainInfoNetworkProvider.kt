package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.API_BLOCKCHAIN_INFO
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.ifNotNullOr
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Calendar

class BlockchainInfoNetworkProvider() : BitcoinNetworkProvider {
    override val baseUrl: String = API_BLOCKCHAIN_INFO

    private val api =
        createRetrofitInstance(API_BLOCKCHAIN_INFO).create(BlockchainInfoApi::class.java)

    private val decimals = Blockchain.Bitcoin.decimals()
    private val responseTransactionCap = 50

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            coroutineScope {
                val addressDeferred = retryIO { async { api.getAddressData(address, null) } }
                val unspentsDeferred = retryIO { async { api.getUnspents(address) } }

                val addressData = addressDeferred.await()
                val unspents = unspentsDeferred.await()

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
                        recentTransactions = addressData.transactions.toRecentTransactions(walletAddress = address),
                    ))
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return try {
            val feeData = retryIO { api.getFees() }
            ifNotNullOr(feeData.regularFeePerByte, feeData.priorityFeePerByte,
                { regular, priority ->
                    val minimalFeePerKb = regular * 1024
                    val normalFeePerKb = (regular + priority) / 2 * 1024
                    val priorityFeePerKb = priority * 1024

                    Result.Success(BitcoinFee(
                        minimalFeePerKb.toBigDecimal().movePointLeft(decimals),
                        normalFeePerKb.toBigDecimal().movePointLeft(decimals),
                        priorityFeePerKb.toBigDecimal().movePointLeft(decimals)
                    ))
                }, {
                    Result.Failure(BlockchainSdkError.FailedToLoadFee)
                })
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO { api.sendTransaction(transaction) }
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
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
                transactions.filter { it.balanceDif!! < 0 }.forEach { signatureCount += it.inputCount!! }

                Result.Success(transactions.size)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private fun List<BlockchainInfoTransaction>?.toRecentTransactions(walletAddress: String): List<BasicTransactionData> {
        return this?.map { it.toBasicTransactionData(walletAddress) } ?: emptyList()
    }

    private fun BlockchainInfoTransaction.toBasicTransactionData(walletAddress: String): BasicTransactionData {
        val balanceDiff = balanceDif ?: 0
        val isIncoming = balanceDiff > 0
        val date = Calendar.getInstance().also { calendar ->
            if (time != null) calendar.timeInMillis = time * 100
        }
        var source = "unknown"
        var destination = "unknown"
        if (isIncoming) {
            inputs
                .firstOrNull { it.previousOutput?.address != walletAddress }
                ?.previousOutput
                ?.address
                ?.let { source = it }
            destination = walletAddress
        } else {
            source = walletAddress
            outputs
                .firstOrNull { it.address != walletAddress }
                ?.address
                ?.let { destination = it }
        }
        return BasicTransactionData(
            balanceDif = balanceDiff.toBigDecimal().movePointLeft(decimals),
            hash = hash.orEmpty(),
            date = date,
            isConfirmed = blockHeight != 0L,
            destination = destination,
            source = source,
        )
    }

    private suspend fun getRemainingTransactions(
        address: String,
        transactionsTotal: Int,
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
            Result.Failure(exception.toBlockchainSdkError())
        }
    }
}
