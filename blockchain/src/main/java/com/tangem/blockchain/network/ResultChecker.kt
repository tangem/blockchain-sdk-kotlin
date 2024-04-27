package com.tangem.blockchain.network

import com.squareup.moshi.JsonDataException
import com.tangem.blockchain.blockchains.ethereum.network.EthereumResponse
import com.tangem.blockchain.blockchains.stellar.StellarNetworkService.Companion.HTTP_NOT_FOUND_CODE
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import org.stellar.sdk.requests.ErrorResponse
import retrofit2.HttpException
import java.io.IOException

object ResultChecker {

    fun needToSwitchProvider(result: Result<*>): Boolean {
        return when (result) {
            is Result.Success -> needToSwitchProviderInternal(result)
            is Result.Failure ->
                when (result.error) {
                    is BlockchainSdkError.WrappedThrowable -> result.error.needToSwitchProvider()
                    is BlockchainSdkError.Solana.Api -> true
                    is BlockchainSdkError.Polkadot.Api -> true
                    is BlockchainSdkError.Ton.Api -> true
                    is BlockchainSdkError.Cosmos.Api -> true
                    is BlockchainSdkError.ElectrumBlockchain.Api -> true
                    else -> false
                }
        }
    }

    fun needToSwitchProvider(result: SimpleResult): Boolean {
        return when (result) {
            is SimpleResult.Success -> false
            is SimpleResult.Failure ->
                when (result.error) {
                    is BlockchainSdkError.WrappedThrowable -> result.error.needToSwitchProvider()
                    is BlockchainSdkError.Solana.Api -> true
                    is BlockchainSdkError.Polkadot.Api -> true
                    is BlockchainSdkError.Ton.Api -> true
                    is BlockchainSdkError.Cosmos.Api -> true
                    is BlockchainSdkError.ElectrumBlockchain.Api -> true
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
        return (result.data as? EthereumResponse)?.error?.message
    }

    private fun needToSwitchProviderInternal(result: Result.Success<*>): Boolean {
        return result.data is EthereumResponse && result.data.error != null
            && result.data.error.code != -32000 // insufficient funds
    }

    private fun BlockchainSdkError.WrappedThrowable.needToSwitchProvider(): Boolean {
        return cause is IOException || cause is HttpException || cause is JsonDataException || stellarNetworkError(
            cause,
        )
    }

    private fun stellarNetworkError(cause: Throwable?): Boolean {
        return cause is ErrorResponse && cause.code != HTTP_NOT_FOUND_CODE
    }
}