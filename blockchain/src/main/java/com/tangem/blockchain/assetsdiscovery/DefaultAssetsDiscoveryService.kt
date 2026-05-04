package com.tangem.blockchain.assetsdiscovery

import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset

internal object DefaultAssetsDiscoveryService : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> = emptyList()
}