package com.tangem.blockchain.network

import com.squareup.moshi.JsonDataException
import com.tangem.blockchain.blockchains.ethereum.network.EthereumResponse
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import retrofit2.HttpException
import java.io.IOException

object ResultChecker {

    fun isNetworkError(result: Result<*>): Boolean {
        return when (result) {
            is Result.Success -> isError(result)
            is Result.Failure ->
                when (result.error) {
                    is BlockchainSdkError.WrappedThrowable -> result.error.isNetworkError()
                    is BlockchainSdkError.Solana.Api -> true
                    is BlockchainSdkError.Polkadot.Api -> true
                    is BlockchainSdkError.Ton.Api -> true
                    is BlockchainSdkError.Cosmos.Api -> true
                    else -> false
                }
        }
    }

    fun isNetworkError(result: SimpleResult): Boolean {
        return when (result) {
            is SimpleResult.Success -> false
            is SimpleResult.Failure ->
                when (result.error) {
                    is BlockchainSdkError.WrappedThrowable -> result.error.isNetworkError()
                    is BlockchainSdkError.Solana.Api -> true
                    is BlockchainSdkError.Polkadot.Api -> true
                    is BlockchainSdkError.Ton.Api -> true
                    is BlockchainSdkError.Cosmos.Api -> true
                    else -> false
                }
        }
    }

    internal fun getErrorMessageIfAvailable(result: Result<*>): String? {
        return when (result) {
            is Result.Failure -> result.error.message
            is Result.Success -> extractEthereumErrorMessage(result)
        }
    }

    internal fun getErrorMessageIfAvailable(result: SimpleResult): String? {
        return when (result) {
            is SimpleResult.Failure -> result.error.message
            SimpleResult.Success -> null
        }
    }

    private fun extractEthereumErrorMessage(result: Result.Success<*>): String? {
        return (result.data as? EthereumResponse)?.error?.message
    }

    private fun isError(result: Result.Success<*>): Boolean {
        return result.data is EthereumResponse && result.data.error != null
    }

    private fun BlockchainSdkError.WrappedThrowable.isNetworkError(): Boolean {
        return cause is IOException || cause is HttpException || cause is JsonDataException
    }
}


