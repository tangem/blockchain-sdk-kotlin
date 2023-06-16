package com.tangem.blockchain.network.blockcypher

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
import com.tangem.blockchain.network.API_BLOCKCYPHER
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BlockcypherNetworkProvider(
        blockchain: Blockchain,
        private val tokens: Set<String>?
) : BitcoinNetworkProvider {

    override val baseUrl: String = getBaseUrl(blockchain)

    private val api: BlockcypherApi by lazy {
        createRetrofitInstance(baseUrl).create(BlockcypherApi::class.java)
    }

    private val transactionHashesCountLimit = 1000
    private val limitCap = 2000
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
    private val decimals = blockchain.decimals()

    override suspend fun getInfo(address: String) = getInfo(address, null)

    suspend fun getInfo(address: String, token: String?): Result<BitcoinAddressInfo> {
        return try {
            val addressData =
                    retryIO { api.getAddressData(
                        address = address,
                        limit = transactionHashesCountLimit,
                        token = token
                    )}

            val confirmedTransactions =
                    addressData.txrefs?.toBasicTransactionsData(isConfirmed = true) ?: emptyList()
            val unconfirmedTransactions =
                    addressData.unconfirmedTxrefs?.toBasicTransactionsData(isConfirmed = false)
                            ?: emptyList()

            val transactions = confirmedTransactions + unconfirmedTransactions

            val unspentOutputs = addressData.txrefs?.filter { it.spent == false }?.map {
                BitcoinUnspentOutput(
                        it.amount!!.toBigDecimal().movePointLeft(decimals),
                        it.outputIndex!!.toLong(),
                        it.hash!!.hexToBytes(),
                        it.outputScript!!.hexToBytes()
                )
            }
            Result.Success(
                    BitcoinAddressInfo(
                            addressData.balance!!.toBigDecimal().movePointLeft(decimals),
                            unspentOutputs ?: emptyList(),
                            transactions,
                            addressData.unconfirmedBalance != 0L
                    )
            )
        } catch (error: HttpException) {
            return if (error.code() == 429 && token == null && !tokens.isNullOrEmpty()) {
                getInfo(address, getToken())
            } else {
                Result.Failure(error.toBlockchainSdkError())
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getFee() = getFee(null)

    suspend fun getFee(token: String?): Result<BitcoinFee> {
        return try {
            val receivedFee: BlockcypherFee =
                    retryIO { api.getFee(token) }
            Result.Success(
                    BitcoinFee(
                            receivedFee.minFeePerKb!!.toBigDecimal().movePointLeft(decimals),
                            receivedFee.normalFeePerKb!!.toBigDecimal().movePointLeft(decimals),
                            receivedFee.priorityFeePerKb!!.toBigDecimal().movePointLeft(decimals)
                    )
            )
        } catch (error: HttpException) {
            return if (error.code() == 429 && token == null && !tokens.isNullOrEmpty()) {
                getFee(getToken())
            } else {
                Result.Failure(error.toBlockchainSdkError())
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        if (tokens.isNullOrEmpty()) {
            return SimpleResult.Failure(
                    BlockchainSdkError.CustomError("Send transaction request is unavailable without a token")
            )
        }
        return try {
            retryIO {
                api.sendTransaction(BlockcypherSendBody(transaction), getToken()!!)
            }
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getSignatureCount(address: String) = getSignatureCount(address, null)

    // TODO: there is a limit of 2000 txrefs, we can miss some transactions if there is more
    suspend fun getSignatureCount(address: String, token: String?): Result<Int> {
        return try {
            val addressData: BlockcypherAddress =
                    retryIO {
                        api.getAddressData(address, limitCap, token)
                    }

            var signatureCount = addressData.txrefs?.filter { it.outputIndex == -1 }?.size ?: 0
            signatureCount += addressData.unconfirmedTxrefs?.filter { it.outputIndex == -1 }?.size
                    ?: 0

            Result.Success(signatureCount)

        } catch (error: HttpException) {
            return if (error.code() == 429 && token == null && !tokens.isNullOrEmpty()) {
                getSignatureCount(address, getToken())
            } else {
                Result.Failure(error.toBlockchainSdkError())
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private fun List<BlockcypherTxref>.toBasicTransactionsData(
            isConfirmed: Boolean
    ): List<BasicTransactionData> {
        val transactionsMap: MutableMap<String, BasicTransactionData> = mutableMapOf()

        this.forEach {
            var balanceDif = if (it.outputIndex == -1) { // outgoing only
                -it.amount!!.toBigDecimal().movePointLeft(decimals)
            } else { // incoming only
                it.amount!!.toBigDecimal().movePointLeft(decimals)
            }
            if (it.hash in transactionsMap) {
                balanceDif += transactionsMap[it.hash]!!.balanceDif
            }

            val date = if (!it.received.isNullOrEmpty()) {
                Calendar.getInstance().apply { time = dateFormat.parse(it.received!!)!! }
            } else {
                null
            }

            val transaction = BasicTransactionData(
                    balanceDif = balanceDif,
                    hash = it.hash!!,
                    date = date,
                    isConfirmed = isConfirmed
            )
            transactionsMap[it.hash] = transaction
        }
        return transactionsMap.values.toList()
    }

    private fun getToken(): String? = tokens?.random()

    private fun getBaseUrl(blockchain: Blockchain): String {
        val apiVersionPath = "v1/"
        val blockchainPath = when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> "btc/"
            Blockchain.Litecoin -> "ltc/"
            Blockchain.Ethereum -> "eth/"
            Blockchain.EthereumClassic -> "etc/"
            Blockchain.Dogecoin -> "doge/"
            Blockchain.Dash -> "dash/"
            else -> throw Exception(
                "${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}"
            )
        }
        val networkPath = when (blockchain) {
            Blockchain.BitcoinTestnet -> "test3/"
            else -> "main/"
        }
        return API_BLOCKCYPHER + apiVersionPath + blockchainPath + networkPath
    }
}