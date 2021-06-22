package com.tangem.blockchain.blockchains.xrp.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

class XrpNetworkService(providers: List<XrpNetworkProvider>) :
        MultiNetworkProvider<XrpNetworkProvider>(providers),
        XrpNetworkProvider {

    override val host: String
        get() = currentProvider.host

    override suspend fun getInfo(address: String): Result<XrpInfoResponse> =
        DefaultRequest(XrpNetworkProvider::getInfo, address).perform()

    override suspend fun sendTransaction(transaction: String): SimpleResult =
        SimpleRequest(XrpNetworkProvider::sendTransaction, transaction).perform()

    override suspend fun getFee(): Result<XrpFeeResponse> =
        NoDataRequest(XrpNetworkProvider::getFee).perform()

    override suspend fun checkIsAccountCreated(address: String): Boolean {
        return currentProvider.checkIsAccountCreated(address)
    }
}