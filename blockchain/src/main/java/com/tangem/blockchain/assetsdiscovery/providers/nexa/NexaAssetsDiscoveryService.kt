package com.tangem.blockchain.assetsdiscovery.providers.nexa

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.blockchains.nexa.NexaAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

internal class NexaAssetsDiscoveryService(
    private val multiNetworkProvider: MultiNetworkProvider<ElectrumNetworkProvider>,
    private val blockchain: Blockchain,
) : AssetsDiscoveryService {

    private val addressService = NexaAddressService(isTestNet = blockchain.isTestnet())

    override suspend fun discoverAssets(walletAddress: String): List<DiscoveredAsset> {
        val scriptHash = runCatching {
            addressService.getScriptPublicKey(walletAddress).calculateSha256().reversedArray().toHexString()
        }.getOrNull() ?: return emptyList()

        val account = when (
            val result = multiNetworkProvider.performRequest(
                request = ElectrumNetworkProvider::getAccountBalance,
                data = scriptHash,
            )
        ) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        if (account.confirmedAmount <= BigDecimal.ZERO) return emptyList()

        return listOf(DiscoveredAsset.Coin(symbol = blockchain.currency, amount = account.confirmedAmount))
    }
}