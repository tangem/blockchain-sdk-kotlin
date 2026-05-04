package com.tangem.blockchain.assetsdiscovery.providers.sui

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.sui.network.SuiConstants
import com.tangem.blockchain.blockchains.sui.network.rpc.SuiJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class SuiAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<SuiJsonRpcProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val coins = when (val result = multiNetworkProvider.performRequest { getCoins(walletAddress) }) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        return coins.groupBy { it.coinType }
            .mapNotNull { (coinType, entries) ->
                val total = entries.fold(BigDecimal.ZERO) { acc, coin -> acc + coin.balance }
                if (total <= BigDecimal.ZERO) return@mapNotNull null
                if (coinType == SuiConstants.COIN_TYPE) {
                    DiscoveredAsset.Coin(
                        symbol = blockchain.currency,
                        amount = total.movePointLeft(blockchain.decimals()),
                    )
                } else {
                    DiscoveredAsset.Token(contractAddress = coinType, amount = total)
                }
            }
    }
}