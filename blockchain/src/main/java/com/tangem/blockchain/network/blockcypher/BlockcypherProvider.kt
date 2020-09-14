package com.tangem.blockchain.network.blockcypher

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
import java.text.SimpleDateFormat
import java.util.*

class BlockcypherProvider(private val api: BlockcypherApi, blockchain: Blockchain) : BitcoinProvider {
    private val limitCap = 2000
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    private val blockchainPath = when (blockchain) {
        Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> "btc"
        Blockchain.Litecoin -> "ltc"
        Blockchain.Ethereum -> "eth"
        else -> throw Exception(
                "${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}"
        )
    }

    private val network = when (blockchain) {
        Blockchain.BitcoinTestnet -> "test3"
        else -> "main"
    }

    private val decimals = blockchain.decimals()

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            val addressData: BlockcypherAddress =
                    retryIO { api.getAddressData(blockchainPath, network, address) }

            val transactions =
                    addressData.txrefs!!.toBasicTransactionsData(isConfirmed = true) +
                            addressData.unconfirmedTxrefs!!.toBasicTransactionsData(isConfirmed = false)

            val unspentOutputs = addressData.txrefs.filter { it.spent == false }.map {
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
                            unspentOutputs,
                            transactions
                    )
            )

        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return try {
            val receivedFee: BlockcypherFee = retryIO { api.getFee(blockchainPath, network) }
            Result.Success(
                    BitcoinFee(
                            receivedFee.minFeePerKb!!.toBigDecimal().movePointLeft(decimals),
                            receivedFee.normalFeePerKb!!.toBigDecimal().movePointLeft(decimals),
                            receivedFee.priorityFeePerKb!!.toBigDecimal().movePointLeft(decimals)
                    )
            )
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO {
                api.sendTransaction(
                        blockchainPath, network, BlockcypherSendBody(transaction), BlockcypherToken.getToken())
            }
            SimpleResult.Success
        } catch (error: Exception) {
            SimpleResult.Failure(error)
        }
    }

    // TODO: there is a limit of 2000 txrefs, we can miss some transactions if there is more
    override suspend fun getSignatureCount(address: String): Result<Int> {
        return try {
            val addressData: BlockcypherAddress =
                    retryIO { api.getAddressData(blockchainPath, network, address, limitCap) }

            var signatureCount = addressData.txrefs!!.filter { it.outputIndex == -1 }.size
            signatureCount += addressData.unconfirmedTxrefs!!.filter { it.outputIndex == -1 }.size

            Result.Success(signatureCount)
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    private fun List<BlockcypherTxref>.toBasicTransactionsData(isConfirmed: Boolean): List<BasicTransactionData> {
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
}

private object BlockcypherToken {
    private val tokens = listOf(
            "aa8184b0e0894b88a5688e01b3dc1e82",
            "56c4ca23c6484c8f8864c32fde4def8d",
            "66a8a37c5e9d4d2c9bb191acfe7f93aa"
    )

    fun getToken(): String = tokens.random()
}