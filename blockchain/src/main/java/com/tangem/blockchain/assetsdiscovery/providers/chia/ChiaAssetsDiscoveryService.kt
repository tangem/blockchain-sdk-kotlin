package com.tangem.blockchain.assetsdiscovery.providers.chia

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.chia.ChiaAddressService
import com.tangem.blockchain.blockchains.chia.network.ChiaNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

internal class ChiaAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<ChiaNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val puzzleHash = ChiaAddressService.getPuzzleHash(walletAddress).toHexString().lowercase()
        val coins = when (val result = multiNetworkProvider.performRequest { getUnspents(puzzleHash) }) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val balance = coins.sumOf { it.amount }.toBigDecimal().movePointLeft(blockchain.decimals())
        if (balance <= BigDecimal.ZERO) return emptyList()

        return listOf(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = balance))
    }
}