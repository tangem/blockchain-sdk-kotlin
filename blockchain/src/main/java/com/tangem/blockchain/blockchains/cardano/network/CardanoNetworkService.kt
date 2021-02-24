package com.tangem.blockchain.blockchains.cardano.network

import com.tangem.blockchain.blockchains.cardano.CardanoUnspentOutput
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

class CardanoNetworkService(providers: List<CardanoNetworkProvider>) :
        MultiNetworkProvider<CardanoNetworkProvider>(providers),
        CardanoNetworkProvider {

    override suspend fun getInfo(addresses: Set<String>): Result<CardanoAddressResponse> {
        val result = provider.getInfo(addresses)
        return if (result.needsRetry()) getInfo(addresses) else result
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        val result = provider.sendTransaction(transaction)
        return if (result.needsRetry()) sendTransaction(transaction) else result
    }
}