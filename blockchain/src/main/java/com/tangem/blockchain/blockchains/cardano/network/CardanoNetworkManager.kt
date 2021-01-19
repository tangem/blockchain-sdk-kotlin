package com.tangem.blockchain.blockchains.cardano.network

import com.tangem.blockchain.blockchains.cardano.CardanoUnspentOutput
import com.tangem.blockchain.blockchains.cardano.network.adalite.AdaliteProvider
import com.tangem.blockchain.blockchains.cardano.network.api.AdaliteApi
import com.tangem.blockchain.common.SendException
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.API_ADALITE
import com.tangem.blockchain.network.API_ADALITE_RESERVE
import com.tangem.blockchain.network.createRetrofitInstance
import retrofit2.HttpException
import java.io.IOException

class CardanoNetworkManager {
    private val adaliteProvider by lazy {
        val api = createRetrofitInstance(API_ADALITE)
                .create(AdaliteApi::class.java)
        AdaliteProvider(api)
    }

    private val adaliteReserveProvider by lazy {
        val api = createRetrofitInstance(API_ADALITE_RESERVE)
                .create(AdaliteApi::class.java)
        AdaliteProvider(api)
    }

    private var provider = adaliteProvider

    private fun changeProvider() {
        provider = if (provider == adaliteProvider) adaliteReserveProvider else adaliteProvider
    }

    suspend fun getInfo(addresses: Set<String>): Result<CardanoAddressResponse> {
        return when (val result = provider.getInfo(addresses)) {
            is Result.Success -> result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    provider.getInfo(addresses)
                } else {
                    result
                }
            }
        }
    }

    suspend fun sendTransaction(transaction: String): SimpleResult {
        when (val result = provider.sendTransaction(transaction)) {
            is SimpleResult.Success -> return result
            is SimpleResult.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return when (val result = provider.sendTransaction(transaction)) {
                        is SimpleResult.Success -> result
                        is SimpleResult.Failure ->
                            if (result.error is HttpException && result.error.code() == 400) {
                                SimpleResult.Failure(SendException(
                                        "Failed to send Cardano transaction: $transaction"
                                ))
                            } else {
                                result
                            }
                    }
                } else {
                    return SimpleResult.Failure(SendException(
                            "Failed to send Cardano transaction: $transaction"
                    ))
                }
            }
        }
    }
}

data class CardanoAddressResponse(
        val balance: Long,
        val unspentOutputs: List<CardanoUnspentOutput>,
        val recentTransactionsHashes: List<String>
)