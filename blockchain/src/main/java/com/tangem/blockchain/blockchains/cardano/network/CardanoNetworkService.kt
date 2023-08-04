package com.tangem.blockchain.blockchains.cardano.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

class CardanoNetworkService(providers: List<CardanoNetworkProvider>) : CardanoNetworkProvider {

    private val multiProvider = MultiNetworkProvider(providers)
    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    override suspend fun getInfo(addresses: Set<String>): Result<CardanoAddressResponse> =
        multiProvider.performRequest(CardanoNetworkProvider::getInfo, addresses)

    override suspend fun sendTransaction(transaction: ByteArray): SimpleResult =
        multiProvider.performRequest(CardanoNetworkProvider::sendTransaction, transaction)
}