package com.tangem.blockchain.assetsdiscovery.providers.xrp

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.blockchains.xrp.network.XrpTokenBalance
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

internal class XrpAssetsDiscoveryService(
    private val networkProvider: XrpNetworkProvider,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val infoResponse = when (val result = networkProvider.getInfo(walletAddress)) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        if (!infoResponse.accountFound) return emptyList()

        val result = mutableListOf<DiscoveredAsset>()

        if (infoResponse.balance > BigDecimal.ZERO) {
            result.add(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = infoResponse.balance))
        }

        infoResponse.tokenBalances
            .filter { it.balance > BigDecimal.ZERO }
            .mapTo(result) { it.toBalance() }

        return result
    }

    private fun XrpTokenBalance.toBalance(): DiscoveredAsset = DiscoveredAsset.Token(
        contractAddress = "$currency.$issuer",
        amount = balance,
    )
}