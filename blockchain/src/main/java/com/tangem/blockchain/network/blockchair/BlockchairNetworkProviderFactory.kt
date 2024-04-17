package com.tangem.blockchain.network.blockchair

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class BlockchairNetworkProviderFactory(
    private val config: BlockchainSdkConfig,
) {

    fun createProviders(blockchain: Blockchain): List<BitcoinNetworkProvider> {
        val credentials = config.blockchairCredentials ?: return emptyList()

        return credentials.apiKey.map { apiKey ->
            BlockchairNetworkProvider(
                blockchain = blockchain,
                apiKey = apiKey,
                authorizationToken = credentials.authToken,
            )
        }
    }
}