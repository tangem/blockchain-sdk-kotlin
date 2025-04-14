package com.tangem.blockchain.blockchains.tezos.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

class TezosNetworkService(providers: List<TezosNetworkProvider>) : TezosNetworkProvider {

    private val multiProvider = MultiNetworkProvider(providers)
    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    override suspend fun getInfo(address: String): Result<TezosInfoResponse> =
        multiProvider.performRequest(TezosNetworkProvider::getInfo, address)

    override suspend fun isPublicKeyRevealed(address: String): Result<Boolean> =
        multiProvider.performRequest(TezosNetworkProvider::isPublicKeyRevealed, address)

    override suspend fun getHeader(): Result<TezosHeader> =
        multiProvider.performRequest(TezosNetworkProvider::getHeader)

    override suspend fun forgeContents(forgeData: TezosForgeData): Result<String> =
        multiProvider.performRequest(TezosNetworkProvider::forgeContents, forgeData)

    override suspend fun checkTransaction(transactionData: TezosTransactionData): SimpleResult =
        multiProvider.performRequest(TezosNetworkProvider::checkTransaction, transactionData)

    override suspend fun sendTransaction(transaction: String): SimpleResult =
        multiProvider.performRequest(TezosNetworkProvider::sendTransaction, transaction)
}
