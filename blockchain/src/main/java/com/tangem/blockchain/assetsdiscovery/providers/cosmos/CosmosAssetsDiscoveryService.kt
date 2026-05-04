package com.tangem.blockchain.assetsdiscovery.providers.cosmos

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class CosmosAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<CosmosRestProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val response = when (val result = multiNetworkProvider.performRequest { balances(walletAddress) }) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val nativeDenom = nativeDenom(blockchain)
        val result = mutableListOf<DiscoveredAsset>()

        response.balances.forEach { cosmosBalance ->
            val amount = cosmosBalance.amount.toBigDecimal().movePointLeft(blockchain.decimals())
            if (amount <= BigDecimal.ZERO) return@forEach

            if (cosmosBalance.denom == nativeDenom) {
                result.add(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = amount))
            } else {
                result.add(DiscoveredAsset.Token(contractAddress = cosmosBalance.denom, amount = amount))
            }
        }

        return result
    }

    private fun nativeDenom(blockchain: Blockchain): String = when (blockchain) {
        Blockchain.Cosmos, Blockchain.CosmosTestnet -> CosmosChain.Cosmos(blockchain.isTestnet()).smallestDenomination
        Blockchain.TerraV1 -> CosmosChain.TerraV1.smallestDenomination
        Blockchain.TerraV2 -> CosmosChain.TerraV2.smallestDenomination
        Blockchain.Sei, Blockchain.SeiTestnet -> CosmosChain.Sei(testnet = blockchain.isTestnet()).smallestDenomination
        else -> error("Unsupported Cosmos blockchain: $blockchain")
    }
}