package com.tangem.blockchain.assetsdiscovery.providers.cardano

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.cardano.CardanoTokenAddressConverter
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.InfoInput
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class CardanoAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<CardanoNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    private val tokenAddressConverter = CardanoTokenAddressConverter()

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val input = InfoInput(addresses = setOf(walletAddress), tokens = emptySet())
        val response = when (val result = multiNetworkProvider.performRequest { getInfo(input) }) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val result = mutableListOf<DiscoveredAsset>()

        val coinBalance = response.balance.toBigDecimal().movePointLeft(blockchain.decimals())
        if (coinBalance > BigDecimal.ZERO) {
            result.add(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = coinBalance))
        }

        response.unspentOutputs
            .flatMap(CardanoUnspentOutput::assets)
            .groupBy { it.policyID + it.assetNameHex }
            .forEach { (assetId, assets) ->
                val contractAddress = tokenAddressConverter.convertToFingerprint(assetId) ?: return@forEach
                val total = assets.sumOf(CardanoUnspentOutput.Asset::amount).toBigDecimal()
                if (total > BigDecimal.ZERO) {
                    result.add(DiscoveredAsset.Token(contractAddress = contractAddress, amount = total))
                }
            }

        return result
    }
}