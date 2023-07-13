package com.tangem.blockchain.blockchains.ducatus.network.bitcore

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// Now it supports only Ducatus, due to Ducatus Api strange behaviour. Transactions aren't checked.
// Don't have too much time to spend on this stillborn coin.
abstract class BitcoreNetworkProvider(val baseUrl: String) : BitcoinNetworkProvider {

    override val host: String = baseUrl

    private val api = createRetrofitInstance(baseUrl).create(BitcoreApi::class.java)

    private val decimals = Blockchain.Ducatus.decimals()

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

                Result.Success(
                    BitcoinAddressInfo(
                        balance = balanceData.confirmed!!.toBigDecimal()
                            .movePointLeft(decimals), // only confirmed balance is returned right
                        unspentOutputs = unspentOutputs,
                        recentTransactions = emptyList(),
                        hasUnconfirmed = balanceData.unconfirmed!! != 0L
                    )
                )
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val response = retryIO { api.sendTransaction(BitcoreSendBody(listOf(transaction))) }
            if (response.txid != null) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(BlockchainSdkError.CustomError("Unknown send transaction error"))
            }
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }
}