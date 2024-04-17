package com.tangem.blockchain.blockchains.hedera

import com.tangem.blockchain.blockchains.hedera.network.HederaMirrorRestProvider
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_HEDERA_ARKHIA_MIRROR
import com.tangem.blockchain.network.API_HEDERA_ARKHIA_MIRROR_TESTNET
import com.tangem.blockchain.network.API_HEDERA_MIRROR_TESTNET

internal class HederaProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<HederaNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<HederaNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> HederaMirrorRestProvider(baseUrl = it.url)
                ProviderType.Hedera.Arkhia -> createArkhiaMirrorProvider(isTestnet = false)
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<HederaNetworkProvider> {
        return listOfNotNull(
            HederaMirrorRestProvider(baseUrl = API_HEDERA_MIRROR_TESTNET),
            createArkhiaMirrorProvider(isTestnet = true),
        )
    }

    private fun createArkhiaMirrorProvider(isTestnet: Boolean): HederaMirrorRestProvider? {
        return config.hederaArkhiaApiKey?.letNotBlank {
            HederaMirrorRestProvider(
                baseUrl = if (isTestnet) API_HEDERA_ARKHIA_MIRROR_TESTNET else API_HEDERA_ARKHIA_MIRROR,
                key = it,
            )
        }
    }
}