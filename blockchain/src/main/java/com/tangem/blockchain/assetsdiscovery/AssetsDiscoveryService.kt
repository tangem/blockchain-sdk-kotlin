package com.tangem.blockchain.assetsdiscovery

import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset

interface AssetsDiscoveryService {

    suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset>
}