package com.tangem.blockchain.blockchains.tezos.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider

class TezosNetworkService(providers: List<TezosNetworkProvider>) :
    MultiNetworkProvider<TezosNetworkProvider>(providers),
    TezosNetworkProvider {

    override val host: String
        get() = currentProvider.host

    override suspend fun getInfo(address: String): Result<TezosInfoResponse> =
        DefaultRequest(TezosNetworkProvider::getInfo, address).perform()

    override suspend fun isPublicKeyRevealed(address: String): Result<Boolean> =
        DefaultRequest(TezosNetworkProvider::isPublicKeyRevealed, address).perform()

    override suspend fun getHeader(): Result<TezosHeader> =
        NoDataRequest(TezosNetworkProvider::getHeader).perform()

    override suspend fun forgeContents(forgeData: TezosForgeData): Result<String> =
        DefaultRequest(TezosNetworkProvider::forgeContents, forgeData).perform()

    override suspend fun checkTransaction(transactionData: TezosTransactionData): SimpleResult =
        SimpleRequest(TezosNetworkProvider::checkTransaction, transactionData).perform()

    override suspend fun sendTransaction(transaction: String): SimpleResult =
        SimpleRequest(TezosNetworkProvider::sendTransaction, transaction).perform()
}