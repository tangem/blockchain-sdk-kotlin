package com.tangem.blockchain.assetsdiscovery.providers.tron

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.tron.network.TronNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

internal class TronAssetsDiscoveryService(
    private val networkService: TronNetworkService,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val accountResponse = when (val result = networkService.getV1Accounts(walletAddress)) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        if (accountResponse.address == null) return emptyList()

        val result = mutableListOf<DiscoveredAsset>()

        val coinBalance = accountResponse.balance?.toBigDecimal()?.movePointLeft(blockchain.decimals())
            ?: BigDecimal.ZERO
        if (coinBalance > BigDecimal.ZERO) {
            result.add(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = coinBalance))
        }

        accountResponse.trc20?.forEach { tokenMap ->
            tokenMap.entries.firstOrNull()?.let { (contractAddress, balanceString) ->
                val balanceValue = balanceString.toBigDecimalOrNull() ?: return@let
                if (balanceValue.compareTo(BigDecimal.ZERO) == 0) return@let
                result.add(DiscoveredAsset.Token(contractAddress = contractAddress, amount = balanceValue))
            }
        }

        return result
    }
}