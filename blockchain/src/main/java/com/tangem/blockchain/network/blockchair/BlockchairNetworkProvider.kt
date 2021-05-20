package com.tangem.blockchain.network.blockchair

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.*
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.API_BLOCKCHAIR
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

open class BlockchairNetworkProvider(
        blockchain: Blockchain,
        private val apiKey: String? = null
) : BitcoinNetworkProvider {

    private val api: BlockchairApi by lazy {
        val blockchainPath = when (blockchain) {
            Blockchain.Bitcoin -> "bitcoin/"
            Blockchain.BitcoinTestnet -> "bitcoin/testnet/"
            Blockchain.BitcoinCash -> "bitcoin-cash/"
            Blockchain.Litecoin -> "litecoin/"
            else -> throw Exception(
                    "${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}"
            )
        }
        createRetrofitInstance(API_BLOCKCHAIR + blockchainPath)
                .create(BlockchairApi::class.java)
    }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.ROOT)
    private val decimals = blockchain.decimals()
    override val supportsRbf = true

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            val blockchairAddress = retryIO {
                api.getAddressData(
                        address = address,
                        transactionDetails = true,
                        limit = 50,
                        key = apiKey
                )
            }

            val addressData = blockchairAddress.data!!.getValue(address)
            val addressInfo = addressData.addressInfo!!
            val script = addressInfo.script!!.hexToBytes()

            val unspentOutputs = addressData.unspentOutputs!!.map {
                BitcoinUnspentOutput(
                        amount = it.value!!.toBigDecimal().movePointLeft(decimals),
                        outputIndex = it.index!!.toLong(),
                        transactionHash = it.transactionHash!!.hexToBytes(),
                        outputScript = script
                )
            }

            val transactions = addressData.transactions!!.map {
                BasicTransactionData(
                        balanceDif = it.balanceDif!!.toBigDecimal().movePointLeft(decimals),
                        hash = it.hash!!,
                        isConfirmed = it.block!! != -1,
                        date = Calendar.getInstance().apply { time = dateFormat.parse(it.time!!)!! }
                )
            }

            Result.Success(BitcoinAddressInfo(
                    balance = addressInfo.balance!!.toBigDecimal().movePointLeft(decimals),
                    unspentOutputs = unspentOutputs,
                    recentTransactions = transactions
            ))
        } catch (error: Exception) {
            Result.Failure(error)
        }

    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return try {
            val stats = retryIO { api.getBlockchainStats(apiKey) }
            val feePerKb = (stats.data!!.feePerByte!! * 1024).toBigDecimal().movePointLeft(decimals)
            Result.Success(BitcoinFee(
                    minimalPerKb = (feePerKb * BigDecimal.valueOf(0.8))
                            .setScale(decimals, RoundingMode.DOWN),
                    normalPerKb = feePerKb.setScale(decimals, RoundingMode.DOWN),
                    priorityPerKb = (feePerKb * BigDecimal.valueOf(1.2))
                            .setScale(decimals, RoundingMode.DOWN)
            ))
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO { api.sendTransaction(BlockchairBody(transaction), apiKey) }
            SimpleResult.Success
        } catch (error: Exception) {
            SimpleResult.Failure(error)
        }
    }

    override suspend fun getTransaction(transactionHash: String): Result<BitcoinTransaction> {
        return try {
            val transactionResponse = retryIO { api.getTransaction(transactionHash, apiKey) }
            val transactionData = transactionResponse.data!!.getValue(transactionHash)
            val transaction = transactionData.transaction!!

            val inputs = transactionData.inputs!!.map {
                BitcoinTransactionInput(
                        unspentOutput = BitcoinUnspentOutput(
                                amount = it.value!!.toBigDecimal().movePointLeft(decimals),
                                outputIndex = it.index!!.toLong(),
                                transactionHash = it.transactionHash!!.hexToBytes(),
                                outputScript = it.script!!.hexToBytes()
                        ),
                        sender = it.recipient!!,
                        sequence = it.sequence!!
                )
            }
            val outputs = transactionData.outputs!!.map {
                BitcoinTransactionOutput(
                        amount = it.value!!.toBigDecimal().movePointLeft(decimals),
                        recipient = it.recipient!!
                )
            }

            Result.Success(
                    BitcoinTransaction(
                            hash = transaction.hash!!,
                            isConfirmed = transaction.block!! > 0,
                            time = Calendar.getInstance()
                                    .apply { time = dateFormat.parse(transaction.time!!)!! },
                            inputs = inputs,
                            outputs = outputs
                    )
            )
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return try {
            val blockchairAddress = retryIO {
                api.getAddressData(
                        address = address,
                        key = apiKey
                )
            }
            val addressInfo = blockchairAddress.data!!.getValue(address).addressInfo!!
            Result.Success(addressInfo.outputCount!! - addressInfo.unspentOutputCount!!)
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }
}