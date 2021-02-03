package com.tangem.blockchain.blockchains.xrp.network

import com.tangem.blockchain.blockchains.xrp.network.rippled.RippledApi
import com.tangem.blockchain.blockchains.xrp.network.rippled.RippledProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.API_RIPPLED
import com.tangem.blockchain.network.API_RIPPLED_RESERVE
import com.tangem.blockchain.network.createRetrofitInstance
import org.stellar.sdk.requests.ErrorResponse
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal

class XrpNetworkManager : XrpNetworkService {
    private val rippledProvider by lazy {
        val api = createRetrofitInstance(API_RIPPLED)
                .create(RippledApi::class.java)
        RippledProvider(api)
    }

    private val rippledReserveProvider by lazy {
        val api = createRetrofitInstance(API_RIPPLED_RESERVE)
                .create(RippledApi::class.java)
        RippledProvider(api)
    }

    var provider = rippledProvider

    private fun changeProvider() {
        provider = if (provider == rippledProvider) rippledReserveProvider else rippledProvider
    }

    override suspend fun getInfo(address: String): Result<XrpInfoResponse> {
        return when (val result = provider.getInfo(address)) {
            is Result.Success -> result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    provider.getInfo(address)
                } else {
                    result
                }
            }
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return when (val result = provider.sendTransaction(transaction)) {
            is SimpleResult.Success -> result
            is SimpleResult.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    provider.sendTransaction(transaction)
                } else {
                    result
                }
            }
        }
    }

    override suspend fun getFee(): Result<XrpFeeResponse> {
        return when (val result = provider.getFee()) {
            is Result.Success -> result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    provider.getFee()
                } else {
                    result
                }
            }
        }
    }

    override suspend fun checkIsAccountCreated(address: String): Boolean {
        return provider.checkIsAccountCreated(address)
    }
}

data class XrpInfoResponse(
        val balance: BigDecimal = BigDecimal.ZERO,
        val sequence: Long = 0,
        val hasUnconfirmed: Boolean = false,
        val reserveBase: BigDecimal,
        val accountFound: Boolean = true
)

data class XrpFeeResponse(
        val minimalFee: BigDecimal,
        val normalFee: BigDecimal,
        val priorityFee: BigDecimal
)