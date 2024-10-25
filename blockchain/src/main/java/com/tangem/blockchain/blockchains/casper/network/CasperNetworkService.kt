package com.tangem.blockchain.blockchains.casper.network

import com.tangem.blockchain.blockchains.casper.models.CasperBalance
import com.tangem.blockchain.blockchains.casper.models.CasperTransaction
import com.tangem.blockchain.blockchains.casper.network.request.CasperTransactionBody
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider

internal class CasperNetworkService(
    providers: List<CasperNetworkProvider>,
) : CasperNetworkProvider {

    override val baseUrl: String
        get() = multiJsonRpcProvider.currentProvider.baseUrl

    private val multiJsonRpcProvider = MultiNetworkProvider(providers)

    override suspend fun getBalance(address: String): Result<CasperBalance> {
        return multiJsonRpcProvider.performRequest(CasperNetworkProvider::getBalance, address)
    }

    override suspend fun putDeploy(body: CasperTransactionBody): Result<CasperTransaction> {
        return multiJsonRpcProvider.performRequest(CasperNetworkProvider::putDeploy, body)
    }
}