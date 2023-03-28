package com.tangem.blockchain.blockchains.ergo.network.api

import com.tangem.blockchain.blockchains.ergo.network.ErgoAddressResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoAddressRequestData
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiBlockResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiSendTransactionResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiUnspentResponse
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider

class ErgoNetworkService(providers: List<ErgoNetworkProvider>) : ErgoNetworkProvider {

    private val multiProvider = MultiNetworkProvider(providers)
    override val host: String
        get() = multiProvider.currentProvider.host

    override suspend fun getInfo(data: ErgoAddressRequestData): Result<ErgoAddressResponse> {
        return multiProvider.performRequest(ErgoNetworkProvider::getInfo, data)
    }

    override suspend fun getLastBlock(): Result<ErgoApiBlockResponse> {
        return multiProvider.performRequest(ErgoNetworkProvider::getLastBlock)
    }

    override suspend fun getUnspent(address: String): Result<List<ErgoApiUnspentResponse>> {
        return multiProvider.performRequest(ErgoNetworkProvider::getUnspent, address)
    }

    override suspend fun sendTransaction(transaction: String): Result<ErgoApiSendTransactionResponse> {
        return multiProvider.performRequest(ErgoNetworkProvider::sendTransaction, transaction)
    }
}
