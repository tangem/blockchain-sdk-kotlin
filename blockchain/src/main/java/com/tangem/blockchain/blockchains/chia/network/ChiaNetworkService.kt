package com.tangem.blockchain.blockchains.chia.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

class ChiaNetworkService(
    chiaNetworkProviders: List<ChiaNetworkProvider>,
    blockchain: Blockchain,
) : ChiaNetworkProvider {

    private val multiProvider = MultiNetworkProvider(chiaNetworkProviders, blockchain)

    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    override suspend fun getUnspents(puzzleHash: String): Result<List<ChiaCoin>> {
        return multiProvider.performRequest(ChiaNetworkProvider::getUnspents, puzzleHash)
    }

    override suspend fun getFeeEstimate(transactionCost: Long): Result<ChiaEstimateFeeResult> {
        return multiProvider.performRequest(ChiaNetworkProvider::getFeeEstimate, transactionCost)
    }

    override suspend fun sendTransaction(transaction: ChiaTransactionBody): SimpleResult {
        return multiProvider.performRequest(ChiaNetworkProvider::sendTransaction, transaction)
    }
}