package com.tangem.blockchain.blockchains.icp.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class ICPNetworkService(networkProviders: List<ICPNetworkProvider>) : ICPNetworkProvider {

    private val multiNetworkProvider = MultiNetworkProvider(networkProviders)
    override val baseUrl: String
        get() = multiNetworkProvider.currentProvider.baseUrl

    override suspend fun getBalance(address: String): Result<BigDecimal> =
        multiNetworkProvider.performRequest(ICPNetworkProvider::getBalance, address)

    override suspend fun signAndSendTransaction(transferWithSigner: ICPTransferWithSigner): Result<Long?> =
        multiNetworkProvider.performRequest(ICPNetworkProvider::signAndSendTransaction, transferWithSigner)
}
