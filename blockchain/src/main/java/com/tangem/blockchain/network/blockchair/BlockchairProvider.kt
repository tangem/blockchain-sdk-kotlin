package com.tangem.blockchain.network.blockchair

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
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

class BlockchairProvider(private val api: BlockchairApi, blockchain: Blockchain) : BitcoinProvider {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss")

    private val blockchainPath = when (blockchain) {
        Blockchain.BitcoinCash -> "bitcoin-cash"
        Blockchain.Litecoin -> "litecoin"
        else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }
    private val decimals = blockchain.decimals()

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            val blockchairAddress = retryIO { api.getAddressData(address, blockchainPath, API_KEY) }

            val addressData = blockchairAddress.data!!.getValue(address)
            val addressInfo = addressData.addressInfo!!
            val script = addressInfo.script!!.hexToBytes()

            val unspentOutputs = addressData.unspentOutputs!!.map {
                BitcoinUnspentOutput(
                        amount = it.amount!!.toBigDecimal().movePointLeft(decimals),
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
            val stats = retryIO { api.getBlockchainStats(blockchainPath, API_KEY) }
            val feePerKb = (stats.data!!.feePerByte!! * 1024).toBigDecimal().movePointLeft(decimals)
            Result.Success(BitcoinFee(
                    minimalPerKb = (feePerKb * BigDecimal.valueOf(0.8)).setScale(decimals, RoundingMode.DOWN),
                    normalPerKb = feePerKb.setScale(decimals, RoundingMode.DOWN),
                    priorityPerKb = (feePerKb * BigDecimal.valueOf(1.2)).setScale(decimals, RoundingMode.DOWN)
            ))
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO { api.sendTransaction(BlockchairBody(transaction), blockchainPath, API_KEY) }
            SimpleResult.Success
        } catch (error: Exception) {
            SimpleResult.Failure(error)
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return try {
            val blockchairAddress = retryIO { api.getAddressData(address, blockchainPath, API_KEY) }
            val addressInfo = blockchairAddress.data!!.getValue(address).addressInfo!!
            Result.Success(addressInfo.outputCount!! - addressInfo.unspentOutputCount!!)
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }
}

private const val API_KEY = "A___0Shpsu4KagE7oSabrw20DfXAqWlT"