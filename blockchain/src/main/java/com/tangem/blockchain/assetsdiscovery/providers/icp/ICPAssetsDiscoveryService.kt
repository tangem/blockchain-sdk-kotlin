package com.tangem.blockchain.assetsdiscovery.providers.icp

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.icp.network.ICPNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class ICPAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<ICPNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val balance = when (
            val result = multiNetworkProvider.performRequest(ICPNetworkProvider::getBalance, walletAddress)
        ) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        if (balance <= BigDecimal.ZERO) return emptyList()

        return listOf(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = balance))
    }
}