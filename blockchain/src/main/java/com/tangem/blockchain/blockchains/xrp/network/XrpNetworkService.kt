package com.tangem.blockchain.blockchains.xrp.network

import com.tangem.blockchain.common.Token
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

    override suspend fun checkIsAccountCreated(address: String): Boolean =
        multiProvider.currentProvider.checkIsAccountCreated(address)

    override suspend fun checkTargetAccount(address: String, token: Token?): Result<XrpTargetAccountResponse> =
        multiProvider.currentProvider.checkTargetAccount(address, token)

    override suspend fun getSequence(address: String): Result<Long> {
        return multiProvider.currentProvider.getSequence(address)
    }

    override suspend fun checkDestinationTagRequired(address: String): Boolean =
        multiProvider.currentProvider.checkDestinationTagRequired(address)
}