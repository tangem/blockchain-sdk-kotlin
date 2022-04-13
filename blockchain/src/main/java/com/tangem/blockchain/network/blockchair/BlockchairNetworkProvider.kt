package com.tangem.blockchain.network.blockchair

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.API_BLOCKCHAIR
import com.tangem.blockchain.network.API_BLOCKCKAIR_TANGEM
import com.tangem.blockchain.network.blockchair.BlockchairApi.Companion.needRetryWithKey
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import retrofit2.HttpException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

open class BlockchairNetworkProvider(
    blockchain: Blockchain,
    private val apiKey: String? = null,
    private val authorizationToken: String? = null
) : BitcoinNetworkProvider {

    override val host: String = createHost(blockchain)

    private val api: BlockchairApi by lazy {
        createRetrofitInstance(host).create(BlockchairApi::class.java)
    }

    private var currentApiKey: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.ROOT)
    private val decimals = blockchain.decimals()

    private fun createHost(blockchain: Blockchain): String {
        val api = if (apiKey != null) API_BLOCKCHAIR else API_BLOCKCKAIR_TANGEM
        return api + getPath(blockchain)
    }

    private suspend fun <T> makeRequestUsingKeyOnlyWhenNeeded(
        block: suspend () -> T
    ): T {
        return try {
            retryIO { block() }
        } catch (error: HttpException) {
            if (needRetryWithKey(error, currentApiKey, apiKey)) {
                currentApiKey = apiKey
                retryIO { block() }
            } else {
                throw error
            }
        }
    }

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            val blockchairAddress = makeRequestUsingKeyOnlyWhenNeeded {
                api.getAddressData(
                    address = address,
                    transactionDetails = true,
                    limit = 50,
                    key = apiKey,
                    authorizationToken = authorizationToken
                )
            }

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

            Result.Success(
                BitcoinAddressInfo(
                    balance = addressInfo.balance!!.toBigDecimal().movePointLeft(decimals),
                    unspentOutputs = unspentOutputs,
                    recentTransactions = transactions
                )
            )
        } catch (error: Exception) {
            Result.Failure(error)
        }

    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return try {
            val stats = makeRequestUsingKeyOnlyWhenNeeded { api.getBlockchainStats(apiKey, authorizationToken) }
            val feePerKb = (stats.data!!.feePerByte!! * 1024).toBigDecimal().movePointLeft(decimals)
            Result.Success(
                BitcoinFee(
                    minimalPerKb = (feePerKb * BigDecimal.valueOf(0.8)).setScale(
                        decimals,
                        RoundingMode.DOWN
                    ),
                    normalPerKb = feePerKb.setScale(decimals, RoundingMode.DOWN),
                    priorityPerKb = (feePerKb * BigDecimal.valueOf(1.2)).setScale(
                        decimals,
                        RoundingMode.DOWN
                    )
                )
            )
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            makeRequestUsingKeyOnlyWhenNeeded {
                api.sendTransaction(BlockchairBody(transaction), apiKey, authorizationToken)
            }
            SimpleResult.Success
        } catch (error: Exception) {
            SimpleResult.Failure(error)
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return try {
            val blockchairAddress = makeRequestUsingKeyOnlyWhenNeeded {
                api.getAddressData(
                    address = address,
                    key = apiKey,
                    authorizationToken = authorizationToken
                )
            }
            val addressInfo = blockchairAddress.data!!.getValue(address).addressInfo!!
            Result.Success(addressInfo.outputCount!! - addressInfo.unspentOutputCount!!)
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    private fun getPath(blockchain: Blockchain): String {
        return when (blockchain) {
            Blockchain.Bitcoin -> "bitcoin/"
            Blockchain.BitcoinTestnet -> "bitcoin/testnet/"
            Blockchain.BitcoinCash -> "bitcoin-cash/"
            Blockchain.Litecoin -> "litecoin/"
            Blockchain.Dogecoin -> "dogecoin/"
            else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
        }
    }
}