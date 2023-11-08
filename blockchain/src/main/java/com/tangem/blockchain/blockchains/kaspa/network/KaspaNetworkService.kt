package com.tangem.blockchain.blockchains.kaspa.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

class KaspaNetworkService(providers: List<KaspaNetworkProvider>) : KaspaNetworkProvider {

    private val multiNetworkProvider = MultiNetworkProvider(providers)
    override val baseUrl: String
        get() = multiNetworkProvider.currentProvider.baseUrl

    override suspend fun getInfo(address: String): Result<KaspaInfoResponse> {
        return multiNetworkProvider.performRequest(KaspaNetworkProvider::getInfo, address)
    }

    override suspend fun sendTransaction(transaction: KaspaTransactionBody): SimpleResult {
        return multiNetworkProvider.performRequest(KaspaNetworkProvider::sendTransaction, transaction)
    }
}
