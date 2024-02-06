package com.tangem.blockchain.blockchains.aptos.network

import com.tangem.blockchain.blockchains.aptos.models.AptosAccountInfo
import com.tangem.blockchain.blockchains.aptos.models.AptosTransactionInfo
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider

/**
 * Aptos network service.
 * Implementation of [AptosNetworkProvider] that wrap network requests in [MultiNetworkProvider].
 *
 * @param providers list of [AptosNetworkProvider]
 *
 * @author Andrew Khokhlov on 11/01/2024
 */
internal class AptosNetworkService(providers: List<AptosNetworkProvider>) : AptosNetworkProvider {

    override val baseUrl: String
        get() = multiJsonRpcProvider.currentProvider.baseUrl

    private val multiJsonRpcProvider = MultiNetworkProvider(providers)

    override suspend fun getAccountInfo(address: String): Result<AptosAccountInfo> {
        return try {
            val accountInfo = multiJsonRpcProvider.performRequest(AptosNetworkProvider::getAccountInfo, address)
                .successOr { return it }

            Result.Success(accountInfo)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getGasUnitPrice(): Result<Long> {
        return try {
            val gasUnitPrice = multiJsonRpcProvider.performRequest(AptosNetworkProvider::getGasUnitPrice)
                .successOr { return it }

            Result.Success(gasUnitPrice)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun calculateUsedGasPriceUnit(transaction: AptosTransactionInfo): Result<Long> {
        return try {
            val usedGasPriceUnit = multiJsonRpcProvider.performRequest(
                request = AptosNetworkProvider::calculateUsedGasPriceUnit,
                data = transaction,
            )
                .successOr { return it }

            Result.Success(usedGasPriceUnit)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun submitTransaction(jsonOutput: String): Result<String> {
        return try {
            val hash = multiJsonRpcProvider.performRequest(AptosNetworkProvider::submitTransaction, jsonOutput)
                .successOr { return it }

            Result.Success(hash)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }
}
