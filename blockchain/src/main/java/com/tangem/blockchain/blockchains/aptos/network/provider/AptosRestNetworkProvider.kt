package com.tangem.blockchain.blockchains.aptos.network.provider

import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.aptos.models.AptosAccountInfo
import com.tangem.blockchain.blockchains.aptos.models.AptosTransactionInfo
import com.tangem.blockchain.blockchains.aptos.network.AptosApi
import com.tangem.blockchain.blockchains.aptos.network.AptosNetworkProvider
import com.tangem.blockchain.blockchains.aptos.network.converter.AptosPseudoTransactionConverter
import com.tangem.blockchain.blockchains.aptos.network.request.AptosTransactionBody
import com.tangem.blockchain.blockchains.aptos.network.request.AptosViewRequest
import com.tangem.blockchain.blockchains.aptos.network.response.AptosResource
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.network.moshi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.math.BigDecimal

internal class AptosRestNetworkProvider(
    override val baseUrl: String,
    private val api: AptosApi = createRetrofitInstance(baseUrl = baseUrl).create(AptosApi::class.java),
) : AptosNetworkProvider {

    override suspend fun getAccountInfo(address: String): Result<AptosAccountInfo> {
        return try {
            coroutineScope {
                val resourcesDeferred = async { api.getAccountResources(address) }
                val balanceDeferred = async { getBalance(address) }

                val resources = resourcesDeferred.await()
                val balance = balanceDeferred.await()

                val accountResource = resources.getResource<AptosResource.AccountResource>()

                if (accountResource == null && balance == BigDecimal.ZERO) {
                    Result.Failure(BlockchainSdkError.AccountNotFound())
                } else {
                    Result.Success(
                        AptosAccountInfo(
                            sequenceNumber = accountResource?.sequenceNumber?.toLong() ?: 0L,
                            balance = balance,
                            tokens = resources.filterIsInstance<AptosResource.TokenResource>(),
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            when {
                e.isNoAccountFoundException() -> Result.Failure(BlockchainSdkError.AccountNotFound())
                e is BlockchainSdkError -> Result.Failure(e)
                else -> Result.Failure(e.toBlockchainSdkError())
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
            if (usedGasUnit != null && usedGasUnit > 0) {
                Result.Success(usedGasUnit)
            } else {
                Result.Failure(BlockchainSdkError.Aptos.Api("Failed to calculate used gas unit"))
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

    /**
     * A failure here must not be turned into a zero balance: the caller cannot distinguish it from an
     * empty account, and [com.tangem.blockchain.network.MultiNetworkProvider] would keep the broken
     * provider instead of switching to the next one.
     */
    private suspend fun getBalance(address: String): BigDecimal {
        val viewRequest = AptosViewRequest(
            function = APTOS_COIN_BALANCE_FUNCTION,
            typeArguments = listOf(APTOS_COIN_CONTRACT),
            arguments = listOf(address),
        )

        val response = api.executeViewFunction(viewRequest)

        return response.firstOrNull()?.toBigDecimalOrNull()
            ?: throw BlockchainSdkError.Aptos.Api("Failed to parse balance from the view function response")
    }

    private companion object {
        const val NOT_FOUND_HTTP_CODE = 404
        const val ACCOUNT_NOT_FOUND_ERROR_CODE = "account_not_found"
        const val APTOS_COIN_BALANCE_FUNCTION = "0x1::coin::balance"
        const val APTOS_COIN_CONTRACT = "0x1::aptos_coin::AptosCoin"
    }
}