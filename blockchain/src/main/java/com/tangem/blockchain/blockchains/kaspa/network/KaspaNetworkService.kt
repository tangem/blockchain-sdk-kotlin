package com.tangem.blockchain.blockchains.kaspa.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider

class KaspaNetworkService(providers: List<KaspaNetworkProvider>) : KaspaNetworkProvider {

    private val multiNetworkProvider = MultiNetworkProvider(providers, Blockchain.Kaspa)
    override val baseUrl: String
        get() = multiNetworkProvider.currentProvider.baseUrl

    override suspend fun getInfo(address: String): Result<KaspaInfoResponse> {
        return multiNetworkProvider.performRequest(KaspaNetworkProvider::getInfo, address)
    }

    override suspend fun sendTransaction(transaction: KaspaTransactionBody): Result<String?> {
        return multiNetworkProvider.performRequest(KaspaNetworkProvider::sendTransaction, transaction)
    }

    override suspend fun calculateFee(transactionData: KaspaTransactionData): Result<KaspaFeeEstimation> {
        return multiNetworkProvider.performRequest(KaspaNetworkProvider::calculateFee, transactionData)
    }
}