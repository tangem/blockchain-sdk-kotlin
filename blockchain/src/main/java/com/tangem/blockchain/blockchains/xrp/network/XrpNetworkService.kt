package com.tangem.blockchain.blockchains.xrp.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

class XrpNetworkService(providers: List<XrpNetworkProvider>) :
        MultiNetworkProvider<XrpNetworkProvider>(providers),
        XrpNetworkProvider {

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