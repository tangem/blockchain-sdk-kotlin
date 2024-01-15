package com.tangem.blockchain.blockchains.hedera.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

class HederaNetworkService(hederaNetworkProviders: List<HederaNetworkProvider>) : HederaNetworkProvider {

    private val multiProvider = MultiNetworkProvider(hederaNetworkProviders)

    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    override suspend fun getAccountId(publicKey: ByteArray): Result<String> {
        return multiProvider.performRequest(HederaNetworkProvider::getAccountId, publicKey)
    }

    override suspend fun getUsdExchangeRate(): Result<BigDecimal> {
        return multiProvider.performRequest(HederaNetworkProvider::getUsdExchangeRate)
    }
}