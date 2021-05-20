package com.tangem.blockchain.blockchains.ducatus.network.bitcore

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinTransaction
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// Now it supports only Ducatus, due to Ducatus Api strange behaviour. Transactions aren't checked.
// Don't have too much time to spend on this stillborn coin.
open class BitcoreNetworkProvider(baseUrl: String) : BitcoinNetworkProvider {

    private val api = createRetrofitInstance(baseUrl).create(BitcoreApi::class.java)

    private val decimals = Blockchain.Ducatus.decimals()
    override val supportsRbf = false

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            coroutineScope {
                val balanceDeferred = retryIO { async { api.getBalance(address) } }
                val unspentsDeferred = retryIO { async { api.getUnspents(address) } }

                val balanceData = balanceDeferred.await()
                val unspents = unspentsDeferred.await()

                val unspentOutputs = unspents.map {
                    BitcoinUnspentOutput(
                            amount = it.amount!!.toBigDecimal().movePointLeft(decimals),
                            outputIndex = it.index!!.toLong(),
                            transactionHash = it.transactionHash!!.hexToBytes(),
                            outputScript = it.script!!.hexToBytes()
                    )
                }

                Result.Success(BitcoinAddressInfo(
                        balance = balanceData.confirmed!!.toBigDecimal().movePointLeft(decimals), // only confirmed balance is returned right
                        unspentOutputs = unspentOutputs,
                        recentTransactions = emptyList(),
                        hasUnconfirmed = balanceData.unconfirmed!! != 0L
                ))
            }
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        // TODO Bitcore is used only in Ducatus and fee is hardcoded there
        return Result.Failure(Exception("Not yet implemented"))
    }


    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val response = retryIO { api.sendTransaction(BitcoreSendBody(listOf(transaction))) }
            if (response.txid != null) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(Exception("Unknown send transaction error"))
            }
        } catch (error: Exception) {
            SimpleResult.Failure(error)
        }
    }

    override suspend fun getTransaction(transactionHash: String): Result<BitcoinTransaction> {
        // TODO Bitcore is used only in Ducatus and we don't need transaction pushing for it
        return Result.Failure(Exception("Not yet implemented"))
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        // TODO Bitcore is used only in Ducatus and we don't check signature count
        return Result.Failure(Exception("Not yet implemented"))
    }
}