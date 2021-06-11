package com.tangem.blockchain.blockchains.cardano.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

class CardanoNetworkService(providers: List<CardanoNetworkProvider>) :
        MultiNetworkProvider<CardanoNetworkProvider>(providers),
        CardanoNetworkProvider {

    override val host: String
        get() = currentProvider.host

    override suspend fun getInfo(addresses: Set<String>): Result<CardanoAddressResponse> {
        val result = currentProvider.getInfo(addresses)
        return if (result.needsRetry()) getInfo(addresses) else result
    }

    override suspend fun sendTransaction(transaction: ByteArray): SimpleResult {
        val result = currentProvider.sendTransaction(transaction)
        return if (result.needsRetry()) sendTransaction(transaction) else result
    }
}