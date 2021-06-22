package com.tangem.blockchain.blockchains.cardano.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

class CardanoNetworkService(providers: List<CardanoNetworkProvider>) :
        MultiNetworkProvider<CardanoNetworkProvider>(providers),
        CardanoNetworkProvider {

    override val host: String
        get() = currentProvider.host

    override suspend fun getInfo(addresses: Set<String>): Result<CardanoAddressResponse> =
        DefaultRequest(CardanoNetworkProvider::getInfo, addresses).perform()

    override suspend fun sendTransaction(transaction: ByteArray): SimpleResult =
        SimpleRequest(CardanoNetworkProvider::sendTransaction, transaction).perform()
}