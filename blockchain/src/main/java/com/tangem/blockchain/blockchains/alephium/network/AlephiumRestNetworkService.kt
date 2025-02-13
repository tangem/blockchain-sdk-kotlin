package com.tangem.blockchain.blockchains.alephium.network

import com.tangem.blockchain.blockchains.alephium.models.AlephiumFee
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import okhttp3.Interceptor
import java.math.BigDecimal

class AlephiumRestNetworkService(
    override val baseUrl: String,
    headerInterceptors: List<Interceptor> = emptyList(),
) : AlephiumNetworkProvider {

    private val api = createRetrofitInstance(baseUrl = baseUrl, headerInterceptors = headerInterceptors)
        .create(AlephiumApi::class.java)

    override suspend fun getInfo(address: String): Result<AlephiumResponse.Utxos> {
        return try {
            val result = retryIO { api.getUtxos(address) }
            Result.Success(result)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getFee(amount: BigDecimal, destination: String, publicKey: String): Result<AlephiumFee> {
        return try {
            val amountString = amount.toPlainString()
            val destinations = listOf(AlephiumRequest.BuildTx.Destination(destination, amountString))
            val request = AlephiumRequest.BuildTx(
                destinations = destinations,
                fromPublicKey = publicKey,
            )
            val response = retryIO { api.buildTx(request) }
            val fee = AlephiumFee(
                gasPrice = response.gasPrice.toBigDecimal(),
                gasAmount = response.gasAmount.toBigDecimal(),
                unsignedTx = response.unsignedTx,
            )
            Result.Success(fee)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun submitTx(unsignedTx: String, signature: String): Result<AlephiumResponse.SubmitTx> {
        return try {
            Result.Success(api.submitTx(request = AlephiumRequest.SubmitTx(unsignedTx, signature)))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }
}