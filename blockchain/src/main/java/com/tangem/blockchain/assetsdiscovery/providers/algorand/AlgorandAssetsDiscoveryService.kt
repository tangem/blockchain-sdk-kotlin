package com.tangem.blockchain.assetsdiscovery.providers.algorand

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.algorand.network.AlgorandNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class AlgorandAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<AlgorandNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val account = when (val result = multiNetworkProvider.performRequest { getAccount(walletAddress) }) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val balance = account.amount.toBigDecimal().movePointLeft(blockchain.decimals())
        if (balance <= BigDecimal.ZERO) return emptyList()

        return listOf(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = balance))
    }
}