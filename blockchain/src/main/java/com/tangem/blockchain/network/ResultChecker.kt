package com.tangem.blockchain.network

import com.squareup.moshi.JsonDataException
import com.tangem.blockchain.blockchains.stellar.StellarNetworkService.Companion.HTTP_NOT_FOUND_CODE
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import org.stellar.sdk.requests.ErrorResponse
import retrofit2.HttpException
import java.io.IOException

object ResultChecker {

    private const val EXECUTION_REVERTED_ERROR_CODE = 3
    private const val EXECUTION_REVERTED_MESSAGE = "execution reverted"

    fun isNetworkError(result: Result<*>): Boolean {
        return when (result) {
            is Result.Success -> isNetworkError(result)
            is Result.Failure ->
                when (result.error) {
                    is BlockchainSdkError.WrappedThrowable -> result.error.isNetworkError()
                    is BlockchainSdkError.Solana.Api -> true
                    is BlockchainSdkError.Polkadot.Api -> true
                    is BlockchainSdkError.Ton.Api -> true
                    is BlockchainSdkError.Cosmos.Api -> true
                    is BlockchainSdkError.ElectrumBlockchain.Api -> true
                    is BlockchainSdkError.Aptos.Api -> true
                    is BlockchainSdkError.Algorand.Send -> true
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
                    is BlockchainSdkError.ElectrumBlockchain.Api -> true
                    is BlockchainSdkError.Aptos.Api -> true
                    is BlockchainSdkError.Algorand.Send -> true
                    else -> false
                }
        }
    }

    internal fun getErrorMessageIfAvailable(result: Result<*>): String? {
        return when (result) {
            is Result.Failure -> result.error.customMessage
            is Result.Success -> extractEthereumErrorMessage(result)
        }
    }

    internal fun getErrorMessageIfAvailable(result: SimpleResult): String? {
        return when (result) {
            is SimpleResult.Failure -> result.error.customMessage
            SimpleResult.Success -> null
        }
    }

    private fun extractEthereumErrorMessage(result: Result.Success<*>): String? {
        return (result.data as? JsonRPCResponse)?.error?.message
    }

    private fun isNetworkError(result: Result.Success<*>): Boolean {
        val response = result.data as? JsonRPCResponse ?: return false
        val error = response.error ?: return false

        return !isContractExecutionError(error.code, error.message)
    }

    private fun isContractExecutionError(code: Int, message: String): Boolean {
        // Code 3 is the standard EIP-1474 code for execution reverted
        if (code == EXECUTION_REVERTED_ERROR_CODE) return true
        if (message.contains(EXECUTION_REVERTED_MESSAGE, ignoreCase = true)) return true
        return false
    }

    private fun BlockchainSdkError.WrappedThrowable.isNetworkError(): Boolean {
        return cause is IOException || cause is HttpException || cause is JsonDataException || stellarNetworkError(
            cause,
        )
    }

    private fun stellarNetworkError(cause: Throwable?): Boolean {
        return cause is ErrorResponse && cause.code != HTTP_NOT_FOUND_CODE
    }
}