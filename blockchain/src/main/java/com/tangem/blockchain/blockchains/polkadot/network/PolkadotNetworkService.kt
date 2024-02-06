package com.tangem.blockchain.blockchains.polkadot.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import io.emeraldpay.polkaj.tx.ExtrinsicContext
import java.math.BigDecimal

class PolkadotNetworkService(providers: List<PolkadotNetworkProvider>) : PolkadotNetworkProvider {
    private val multiProvider = MultiNetworkProvider(providers)
    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    override suspend fun getBalance(address: String): Result<BigDecimal> =
        multiProvider.performRequest(PolkadotNetworkProvider::getBalance, address)

    override suspend fun getFee(builtTransaction: ByteArray): Result<BigDecimal> =
        multiProvider.performRequest(PolkadotNetworkProvider::getFee, builtTransaction)

    override suspend fun sendTransaction(builtTransaction: ByteArray): Result<String> =
        multiProvider.performRequest(PolkadotNetworkProvider::sendTransaction, builtTransaction)

    override suspend fun extrinsicContext(address: String): Result<ExtrinsicContext> =
        multiProvider.performRequest(PolkadotNetworkProvider::extrinsicContext, address)
}
