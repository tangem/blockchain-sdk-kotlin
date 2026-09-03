package com.tangem.blockchain.assetsdiscovery.providers.evm

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.network.EthereumLikeJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

/**
 * Coins-only EVM assets discovery service.
 *
 * Used as a fallback for EVM blockchains that are not supported by Moralis:
 * it queries native coin balance via standard `eth_getBalance` (or the equivalent
 * `getBalance` of [EthereumLikeJsonRpcProvider]) and does not enumerate tokens.
 */
internal class DefaultEvmAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<out EthereumLikeJsonRpcProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val response = when (
            val result = multiNetworkProvider.performRequest(
                request = EthereumLikeJsonRpcProvider::getBalance,
                data = walletAddress,
            )
        ) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val rawBalance = response.result as? String ?: return emptyList()
        val balance = EthereumUtils.parseEthereumDecimal(
            value = rawBalance,
            decimalsCount = blockchain.decimals(),
        ) ?: return emptyList()

        if (balance <= BigDecimal.ZERO) return emptyList()

        return listOf(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = balance))
    }
}