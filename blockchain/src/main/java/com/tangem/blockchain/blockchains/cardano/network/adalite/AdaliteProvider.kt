package com.tangem.blockchain.blockchains.cardano.network.adalite

import com.squareup.moshi.Json
import com.tangem.blockchain.blockchains.cardano.CardanoUnspentOutput
import com.tangem.blockchain.blockchains.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkService
import com.tangem.blockchain.blockchains.cardano.network.api.AdaliteApi
import com.tangem.blockchain.common.SendException
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException

class AdaliteProvider(baseUrl: String) : CardanoNetworkService {

    private val api: AdaliteApi by lazy {
        createRetrofitInstance(baseUrl).create(AdaliteApi::class.java)
    }

    override suspend fun getInfo(addresses: Set<String>): Result<CardanoAddressResponse> {
        return try {
            coroutineScope {
                val addressesDeferred = addresses.map { retryIO { async { api.getAddress(it) } } }
                val unspentsDeferred = retryIO { async { api.getUnspents(addresses.toList()) } }

                val addressesData = addressesDeferred.map { it.await() }
                val unspents = unspentsDeferred.await()

                val cardanoUnspents = unspents.data!!.map {
                    CardanoUnspentOutput(
                            it.address!!,
                            it.amountData!!.amount!!,
                            it.outputIndex!!.toLong(),
                            it.hash!!.hexToBytes()
                    )
                }
                val recentTransactionsHashes = addressesData
                        .flatMap { it.data!!.transactions!!.mapNotNull { it.hash } }

                Result.Success(
                        CardanoAddressResponse(
                                addressesData.map { it.data!!.balanceData!!.amount!! }.sum(),
                                cardanoUnspents,
                                recentTransactionsHashes
                        )
                )
            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO { api.sendTransaction(AdaliteSendBody(transaction)) }
            SimpleResult.Success
        } catch (exception: Exception) {
            if (exception is HttpException && exception.code() == 400) {
                SimpleResult.Failure(
                        SendException("Failed to send Cardano transaction: $transaction")
                )
            } else {
                SimpleResult.Failure(exception)
            }
        }
    }
}

data class AdaliteSendBody(
        @Json(name = "signedTx")
        val signedTransaction: String
)