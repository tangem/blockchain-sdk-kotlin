package com.tangem.blockchain.assetsdiscovery.providers.aptos

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.aptos.models.AptosAccountInfo
import com.tangem.blockchain.blockchains.aptos.network.AptosNetworkProvider
import com.tangem.blockchain.blockchains.aptos.network.response.AptosResource
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class AptosAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<AptosNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val info: AptosAccountInfo = when (
            val result = multiNetworkProvider.performRequest { getAccountInfo(walletAddress) }
        ) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val result = mutableListOf<DiscoveredAsset>()

        if (info.balance > BigDecimal.ZERO) {
            result.add(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = info.balance))
        }

        info.tokens
            .filter { it.balance > BigDecimal.ZERO }
            .mapTo(result) { it.toBalance() }

        return result
    }

    private fun AptosResource.TokenResource.toBalance(): DiscoveredAsset = DiscoveredAsset.Token(
        contractAddress = contractAddress,
        amount = balance,
    )
}