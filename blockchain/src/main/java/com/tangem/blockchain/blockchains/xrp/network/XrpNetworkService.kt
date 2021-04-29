package com.tangem.blockchain.blockchains.xrp.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

class XrpNetworkService(providers: List<XrpNetworkProvider>) :
        MultiNetworkProvider<XrpNetworkProvider>(providers),
        XrpNetworkProvider {

    override suspend fun getInfo(address: String): Result<XrpInfoResponse> {
        val result = currentProvider.getInfo(address)
        return if (result.needsRetry()) getInfo(address) else result
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        val result = currentProvider.sendTransaction(transaction)
        return if (result.needsRetry()) sendTransaction(transaction) else result
    }

    override suspend fun getFee(): Result<XrpFeeResponse> {
        val result = currentProvider.getFee()
        return if (result.needsRetry()) getFee() else result
    }

    override suspend fun checkIsAccountCreated(address: String): Boolean {
        return currentProvider.checkIsAccountCreated(address)
    }
}