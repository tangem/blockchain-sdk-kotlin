package com.tangem.blockchain.assetsdiscovery.providers.solana

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.solana.SolanaNetworkService
import com.tangem.blockchain.blockchains.solana.solanaj.model.SolanaTokenAccountInfo
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import org.p2p.solanaj.core.PublicKey
import java.math.BigDecimal

internal class SolanaAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<SolanaNetworkService>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val account = PublicKey(walletAddress)
        val mainAccountInfo = when (
            val result = multiNetworkProvider.performRequest {
                getMainAccountInfo(account, cardTokens = null)
            }
        ) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        if (mainAccountInfo.value == null) return emptyList()

        val result = mutableListOf<DiscoveredAsset>()

        val coinBalance = BigDecimal(mainAccountInfo.balance).movePointLeft(blockchain.decimals())
        if (coinBalance > BigDecimal.ZERO) {
            result.add(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = coinBalance))
        }

        mainAccountInfo.tokensByMint.values
            .filter { it.solAmount > BigDecimal.ZERO }
            .mapTo(result) { it.toBalance() }

        return result
    }

    private fun SolanaTokenAccountInfo.toBalance(): DiscoveredAsset = DiscoveredAsset.Token(
        contractAddress = mint,
        amount = solAmount,
    )
}