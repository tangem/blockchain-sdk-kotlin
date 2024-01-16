package com.tangem.blockchain.blockchains.aptos.network.provider

import com.tangem.blockchain.blockchains.aptos.models.AptosAccountInfo
import com.tangem.blockchain.blockchains.aptos.models.AptosTransactionInfo
import com.tangem.blockchain.blockchains.aptos.network.AptosApi
import com.tangem.blockchain.blockchains.aptos.network.AptosNetworkProvider
import com.tangem.blockchain.blockchains.aptos.network.converter.AptosTransactionConverter
import com.tangem.blockchain.blockchains.aptos.network.response.AptosResourceBody
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.createRetrofitInstance
import okhttp3.Interceptor

internal class AptosJsonRpcNetworkProvider(
    override val baseUrl: String,
    headerInterceptors: List<Interceptor>,
    private val decimals: Int,
) : AptosNetworkProvider {

    private val api = createRetrofitInstance(baseUrl = baseUrl, headerInterceptors = headerInterceptors)
        .create(AptosApi::class.java)

    override suspend fun getAccountInfo(address: String): Result<AptosAccountInfo> {
        return try {
            val resources = api.getAccountResources(address)

            val accountResource = resources.getResource<AptosResourceBody.AccountResource>()?.account
            val coinResource = resources.getResource<AptosResourceBody.CoinResource>()?.coinData

            if (accountResource != null && coinResource != null) {
                Result.Success(
                    AptosAccountInfo(
                        sequenceNumber = accountResource.sequenceNumber.toLong(),
                        balance = coinResource.coin.value.toBigDecimal().movePointLeft(decimals),
                    ),
                )
            } else {
                Result.Failure(BlockchainSdkError.AccountNotFound)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getGasUnitPrice(): Result<Long> {
        return try {
            val response = api.estimateGasPrice()

            Result.Success(response.normalGasUnitPrice)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun calculateUsedGasPriceUnit(transaction: AptosTransactionInfo): Result<Long> {
        return try {
            val requestBody = AptosTransactionConverter.convert(transaction)
            val response = api.simulateTransaction(requestBody).firstOrNull()

            val usedGasUnit = response?.usedGasUnit?.toLongOrNull()
            if (response != null && usedGasUnit != null && response.isSuccess) {
                Result.Success(usedGasUnit)
            } else {
                Result.Failure(BlockchainSdkError.FailedToLoadFee)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun encodeTransaction(transaction: AptosTransactionInfo): Result<String> {
        return try {
            val requestBody = AptosTransactionConverter.convert(transaction)
            val hash = api.encodeSubmission(requestBody)

            if (hash.isNotBlank()) {
                Result.Success(hash)
            } else {
                Result.Failure(BlockchainSdkError.FailedToSendException)
            }

            Result.Success(hash)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun submitTransaction(transaction: AptosTransactionInfo): Result<String> {
        return try {
            val requestBody = AptosTransactionConverter.convert(transaction)
            val response = api.submitTransaction(requestBody)

            if (response.hash.isNotBlank()) {
                Result.Success(response.hash)
            } else {
                Result.Failure(BlockchainSdkError.FailedToSendException)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private inline fun <reified T : AptosResourceBody> List<AptosResourceBody>.getResource(): T? {
        return firstNotNullOfOrNull { it as? T }
    }
}