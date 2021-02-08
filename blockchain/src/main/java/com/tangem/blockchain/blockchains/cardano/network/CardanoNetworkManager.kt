package com.tangem.blockchain.blockchains.cardano.network

import com.tangem.blockchain.blockchains.cardano.CardanoUnspentOutput
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiProvider

class CardanoNetworkManager(providers: List<CardanoNetworkService>) :
        MultiProvider<CardanoNetworkService>(providers),
        CardanoNetworkService {

    override suspend fun getInfo(addresses: Set<String>): Result<CardanoAddressResponse> {
        val result = provider.getInfo(addresses)
        return if (result.needsRetry()) getInfo(addresses) else result
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        val result = provider.sendTransaction(transaction)
        return if (result.needsRetry()) sendTransaction(transaction) else result
    }
}

data class CardanoAddressResponse(
        val balance: Long,
        val unspentOutputs: List<CardanoUnspentOutput>,
        val recentTransactionsHashes: List<String>
)