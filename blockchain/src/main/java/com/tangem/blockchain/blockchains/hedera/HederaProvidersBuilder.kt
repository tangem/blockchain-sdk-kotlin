package com.tangem.blockchain.blockchains.hedera

import com.tangem.blockchain.blockchains.hedera.network.HederaMirrorRestProvider
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_HEDERA_ARKHIA_MIRROR
import com.tangem.blockchain.network.API_HEDERA_ARKHIA_MIRROR_TESTNET
import com.tangem.blockchain.network.API_HEDERA_MIRROR
import com.tangem.blockchain.network.API_HEDERA_MIRROR_TESTNET

internal class HederaProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<HederaNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Hedera, Blockchain.HederaTestnet)

    override fun createProviders(blockchain: Blockchain): List<HederaNetworkProvider> {
        val isTestnet = blockchain.isTestnet()

        return listOfNotNull(
            HederaMirrorRestProvider(if (isTestnet) API_HEDERA_MIRROR_TESTNET else API_HEDERA_MIRROR),

            config.hederaArkhiaApiKey?.letNotBlank {
                HederaMirrorRestProvider(
                    baseUrl = if (isTestnet) API_HEDERA_ARKHIA_MIRROR_TESTNET else API_HEDERA_ARKHIA_MIRROR,
                    key = it,
                )
            },
        )
    }
}