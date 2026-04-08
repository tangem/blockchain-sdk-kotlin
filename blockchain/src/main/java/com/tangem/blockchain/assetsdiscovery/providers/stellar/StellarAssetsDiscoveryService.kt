package com.tangem.blockchain.assetsdiscovery.providers.stellar

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.stellar.StellarAssetBalance
import com.tangem.blockchain.blockchains.stellar.StellarNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class StellarAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<StellarNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val response = when (val result = multiNetworkProvider.performRequest { getInfo(walletAddress) }) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val result = mutableListOf<DiscoveredAsset>()

        if (response.coinBalance > BigDecimal.ZERO) {
            result.add(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = response.coinBalance))
        }

        response.tokenBalances
            .filter { it.balance > BigDecimal.ZERO }
            .mapTo(result) { it.toBalance() }

        return result
    }

    private fun StellarAssetBalance.toBalance(): DiscoveredAsset = DiscoveredAsset.Token(
        contractAddress = "$symbol:$issuer",
        amount = balance,
    )
}