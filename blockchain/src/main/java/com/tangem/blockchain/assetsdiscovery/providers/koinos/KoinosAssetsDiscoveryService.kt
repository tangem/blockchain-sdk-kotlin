package com.tangem.blockchain.assetsdiscovery.providers.koinos

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.koinos.KoinosContractIdHolder
import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class KoinosAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<KoinosNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    private val contractIdHolder = KoinosContractIdHolder(
        isTestnet = blockchain.isTestnet(),
        loadKoinContractId = { multiNetworkProvider.performRequest { getKoinContractId() } },
    )

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val koinContractId = contractIdHolder.get()
        val balanceLong = when (
            val result = multiNetworkProvider.performRequest { getKoinBalance(walletAddress, koinContractId) }
        ) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        val balance = balanceLong.toBigDecimal().movePointLeft(blockchain.decimals())
        if (balance <= BigDecimal.ZERO) return emptyList()

        return listOf(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = balance))
    }
}