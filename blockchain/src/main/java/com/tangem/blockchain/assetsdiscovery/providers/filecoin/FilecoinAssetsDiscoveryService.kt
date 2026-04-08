package com.tangem.blockchain.assetsdiscovery.providers.filecoin

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.filecoin.network.FilecoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class FilecoinAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<FilecoinNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val accountInfo = when (val result = multiNetworkProvider.performRequest { getAccountInfo(walletAddress) }) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val balance = accountInfo.balance.movePointLeft(blockchain.decimals())
        if (balance <= BigDecimal.ZERO) return emptyList()

        return listOf(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = balance))
    }
}