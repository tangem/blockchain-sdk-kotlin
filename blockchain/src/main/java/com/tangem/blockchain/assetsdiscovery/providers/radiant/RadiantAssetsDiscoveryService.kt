package com.tangem.blockchain.assetsdiscovery.providers.radiant

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.radiant.RadiantAddressUtils
import com.tangem.blockchain.blockchains.radiant.network.RadiantNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

internal class RadiantAssetsDiscoveryService(
    private val networkService: RadiantNetworkService,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val scriptHash = RadiantAddressUtils.generateAddressScriptHash(walletAddress)
        val info = when (val result = networkService.getInfo(scriptHash)) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        if (info.balance <= BigDecimal.ZERO) return emptyList()

        return listOf(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = info.balance))
    }
}