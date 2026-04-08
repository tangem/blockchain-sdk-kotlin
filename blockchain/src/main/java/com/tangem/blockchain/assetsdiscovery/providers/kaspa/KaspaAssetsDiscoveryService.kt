package com.tangem.blockchain.assetsdiscovery.providers.kaspa

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class KaspaAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<KaspaNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val info = when (val result = multiNetworkProvider.performRequest { getInfo(walletAddress) }) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val result = mutableListOf<DiscoveredAsset>()

        if (info.balance > BigDecimal.ZERO) {
            result.add(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = info.balance))
        }

        return result
    }
}