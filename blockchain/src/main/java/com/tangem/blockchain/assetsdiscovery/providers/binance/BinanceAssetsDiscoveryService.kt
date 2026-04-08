package com.tangem.blockchain.assetsdiscovery.providers.binance

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

internal class BinanceAssetsDiscoveryService(
    private val networkProvider: BinanceNetworkProvider,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val response = when (val result = networkProvider.getInfo(walletAddress)) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        return response.balances
            .filter { (_, amount) -> amount > BigDecimal.ZERO }
            .map { (symbol, amount) ->
                if (symbol == blockchain.currency) {
                    DiscoveredAsset.Coin(symbol = symbol, amount = amount)
                } else {
                    DiscoveredAsset.Token(contractAddress = symbol, amount = amount)
                }
            }
    }
}