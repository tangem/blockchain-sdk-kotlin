package com.tangem.blockchain.assetsdiscovery.providers.hedera

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class HederaAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<HederaNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val response = when (val result = multiNetworkProvider.performRequest { getBalances(walletAddress) }) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val balanceEntry = response.balances.firstOrNull() ?: return emptyList()

        val result = mutableListOf<DiscoveredAsset>()

        val coinBalance = balanceEntry.balance.toBigDecimal().movePointLeft(blockchain.decimals())
        if (coinBalance > BigDecimal.ZERO) {
            result.add(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = coinBalance))
        }

        balanceEntry.tokenBalances.forEach { tokenBalance ->
            val amount = tokenBalance.balance.toBigDecimal()
            if (amount > BigDecimal.ZERO) {
                result.add(DiscoveredAsset.Token(contractAddress = tokenBalance.tokenId, amount = amount))
            }
        }

        return result
    }
}