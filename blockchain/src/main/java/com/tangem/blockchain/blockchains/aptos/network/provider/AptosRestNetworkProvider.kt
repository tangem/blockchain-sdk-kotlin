package com.tangem.blockchain.blockchains.aptos.network.provider

import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.aptos.models.AptosAccountInfo
import com.tangem.blockchain.blockchains.aptos.models.AptosTransactionInfo
import com.tangem.blockchain.blockchains.aptos.network.AptosApi
import com.tangem.blockchain.blockchains.aptos.network.AptosNetworkProvider
import com.tangem.blockchain.blockchains.aptos.network.converter.AptosPseudoTransactionConverter
import com.tangem.blockchain.blockchains.aptos.network.request.AptosTransactionBody
import com.tangem.blockchain.blockchains.aptos.network.response.AptosResource
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.network.moshi
import retrofit2.HttpException

internal class AptosRestNetworkProvider(override val baseUrl: String) : AptosNetworkProvider {

    private val api = createRetrofitInstance(baseUrl = baseUrl).create(AptosApi::class.java)

    override suspend fun getAccountInfo(address: String): Result<AptosAccountInfo> {
        return try {
            val resources = api.getAccountResources(address)

            val accountResource = resources.getResource<AptosResource.AccountResource>()
            val coinResource = resources.getResource<AptosResource.CoinResource>()

            if (accountResource != null && coinResource != null) {
                Result.Success(
                    AptosAccountInfo(
                        sequenceNumber = accountResource.sequenceNumber.toLong(),
                        balance = coinResource.balance,
                        tokens = resources.filterIsInstance<AptosResource.TokenResource>(),
                    ),
                )
            } else {
                Result.Failure(BlockchainSdkError.AccountNotFound)
            }
        } catch (e: Exception) {
            if (e.isNoAccountFoundException()) {
                Result.Failure(BlockchainSdkError.AccountNotFound)
            } else {
                Result.Failure(e.toBlockchainSdkError())
            }
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
            val requestBody = AptosPseudoTransactionConverter.convert(transaction)
            val response = api.simulateTransaction(requestBody).firstOrNull()

            val usedGasUnit = response?.usedGasUnit?.toLongOrNull()
            if (usedGasUnit != null && response.isSuccess) {
                Result.Success(usedGasUnit)
            } else {
                Result.Failure(BlockchainSdkError.FailedToLoadFee)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun submitTransaction(jsonOutput: String): Result<String> {
        return try {
            val body = moshi.adapter<AptosTransactionBody>().fromJson(jsonOutput)
                ?: return Result.Failure(BlockchainSdkError.FailedToSendException)

            val response = api.submitTransaction(body)

            if (response.hash.isNotBlank()) {
                Result.Success(response.hash)
            } else {
                Result.Failure(BlockchainSdkError.FailedToSendException)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private inline fun <reified T : AptosResource> List<AptosResource>.getResource(): T? {
        return firstNotNullOfOrNull { it as? T }
    }

    private fun Exception.isNoAccountFoundException(): Boolean {
        return this is HttpException && code() == NOT_FOUND_HTTP_CODE &&
            response()?.errorBody()?.string()?.contains(ACCOUNT_NOT_FOUND_ERROR_CODE) == true
    }

    private companion object {
        const val NOT_FOUND_HTTP_CODE = 404
        const val ACCOUNT_NOT_FOUND_ERROR_CODE = "account_not_found"
    }
}
