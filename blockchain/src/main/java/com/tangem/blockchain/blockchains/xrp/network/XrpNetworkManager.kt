package com.tangem.blockchain.blockchains.xrp.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiProvider
import java.math.BigDecimal

class XrpNetworkManager(providers: List<XrpNetworkService>) :
        MultiProvider<XrpNetworkService>(providers),
        XrpNetworkService {

    override suspend fun getInfo(address: String): Result<XrpInfoResponse> {
        val result = provider.getInfo(address)
        return if (result.needsRetry()) getInfo(address) else result
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        val result = provider.sendTransaction(transaction)
        return if (result.needsRetry()) sendTransaction(transaction) else result
    }

    override suspend fun getFee(): Result<XrpFeeResponse> {
        val result = provider.getFee()
        return if (result.needsRetry()) getFee() else result
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