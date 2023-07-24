package com.tangem.blockchain.blockchains.cardano.network.adalite

import com.squareup.moshi.Json
import com.tangem.blockchain.blockchains.cardano.CardanoUnspentOutput
import com.tangem.blockchain.blockchains.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.encodeBase64NoWrap
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.io.IOException

class AdaliteNetworkProvider(baseUrl: String) : CardanoNetworkProvider {

    override val baseUrl: String = baseUrl

    private val api: AdaliteApi by lazy {
        createRetrofitInstance(baseUrl).create(AdaliteApi::class.java)
    }

    override suspend fun getInfo(addresses: Set<String>): Result<CardanoAddressResponse> {
        return try {
            coroutineScope {
                val addressesDeferred = addresses.map { retryIO { async { api.getAddressData(it) } } }
                val unspentsDeferred = retryIO { async { api.getUnspents(addresses.toList()) } }

                val addressesData = addressesDeferred.map { it.await() }
                val unspents = unspentsDeferred.await()

                val cardanoUnspents = unspents.data
                    // we need to ignore unspents with tokens (until we start supporting tokens)
                    .filter { it.amountData.tokens.isEmpty() }
                    .map {
                        CardanoUnspentOutput(
                            address = it.address,
                            amount = it.amountData.amount,
                            outputIndex = it.outputIndex.toLong(),
                            transactionHash = it.hash.hexToBytes()
                        )
                    }
                val recentTransactionsHashes = addressesData
                    .flatMap { it.data!!.transactions!!.mapNotNull { it.hash } }

                Result.Success(
                    CardanoAddressResponse(
                        balance = cardanoUnspents.sumOf { it.amount },
                        unspentOutputs = cardanoUnspents,
                        recentTransactionsHashes = recentTransactionsHashes
                    )
                )
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: ByteArray): SimpleResult {
        return try {
            retryIO { api.sendTransaction(AdaliteSendBody(transaction.encodeBase64NoWrap())) }
            SimpleResult.Success
        } catch (exception: Exception) {
            if (exception is HttpException && exception.code() == 400) {
                val error = IOException(
                    "${Blockchain.CardanoShelley}. Failed to send transaction ${transaction.toHexString()}\nwith an error: " +
                        "\n${exception.response()?.errorBody()?.string()}"
                )
                SimpleResult.Failure(error.toBlockchainSdkError())
            } else {
                SimpleResult.Failure(exception.toBlockchainSdkError())
            }
        }
    }
}

data class AdaliteSendBody(
    @Json(name = "signedTx")
    val signedTransaction: String,
)