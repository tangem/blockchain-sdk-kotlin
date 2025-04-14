package com.tangem.blockchain.blockchains.xrp.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

class XrpNetworkService(providers: List<XrpNetworkProvider>) : XrpNetworkProvider {

    private val multiProvider = MultiNetworkProvider(providers)
    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    override suspend fun getInfo(address: String): Result<XrpInfoResponse> =
        multiProvider.performRequest(XrpNetworkProvider::getInfo, address)

    override suspend fun sendTransaction(transaction: String): SimpleResult =
        multiProvider.performRequest(XrpNetworkProvider::sendTransaction, transaction)

    override suspend fun getFee(): Result<XrpFeeResponse> = multiProvider.performRequest(XrpNetworkProvider::getFee)

    override suspend fun checkIsAccountCreated(address: String): Boolean {
        return multiProvider.currentProvider.checkIsAccountCreated(address)
    }
}
