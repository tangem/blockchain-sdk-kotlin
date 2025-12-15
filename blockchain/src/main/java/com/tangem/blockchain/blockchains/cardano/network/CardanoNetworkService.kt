package com.tangem.blockchain.blockchains.cardano.network

import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoAddressResponse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

internal class CardanoNetworkService(
    providers: List<CardanoNetworkProvider>,
    blockchain: Blockchain,
) : CardanoNetworkProvider {

    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    private val multiProvider = MultiNetworkProvider(providers, blockchain)

    override suspend fun getInfo(input: InfoInput): Result<CardanoAddressResponse> {
        return multiProvider.performRequest(CardanoNetworkProvider::getInfo, input)
    }

    override suspend fun sendTransaction(transaction: ByteArray): SimpleResult {
        return multiProvider.performRequest(CardanoNetworkProvider::sendTransaction, transaction)
    }
}