package com.tangem.blockchain.assetsdiscovery.providers.alephium

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.alephium.network.AlephiumNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class AlephiumAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<AlephiumNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val utxos = when (val result = multiNetworkProvider.performRequest { getInfo(walletAddress) }) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val balance = utxos.utxos
            .sumOf { it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO }
            .movePointLeft(blockchain.decimals())

        if (balance <= BigDecimal.ZERO) return emptyList()

        return listOf(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = balance))
    }
}