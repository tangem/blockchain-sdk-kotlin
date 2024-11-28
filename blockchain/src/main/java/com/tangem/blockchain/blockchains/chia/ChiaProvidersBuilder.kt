package com.tangem.blockchain.blockchains.chia

import com.tangem.blockchain.blockchains.chia.network.ChiaJsonRpcProvider
import com.tangem.blockchain.blockchains.chia.network.ChiaNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_CHIA3_TANGEM
import com.tangem.blockchain.network.API_CHIA_FIREACADEMY
import com.tangem.blockchain.network.API_CHIA_FIREACADEMY_TESTNET
import com.tangem.blockchain.network.API_CHIA_TANGEM

internal class ChiaProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<ChiaNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<ChiaNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                ProviderType.Chia.Tangem -> createTangemProvider()
                ProviderType.Chia.TangemNew -> createTangemNewProvider()
                ProviderType.Chia.FireAcademy -> createFireAcademyProvider(isTestnet = false)
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<ChiaNetworkProvider> {
        return listOfNotNull(
            createFireAcademyProvider(isTestnet = true),
        )
    }

    private fun createTangemProvider(): ChiaNetworkProvider? {
        return config.chiaTangemApiKey?.letNotBlank {
            ChiaJsonRpcProvider(
                baseUrl = API_CHIA_TANGEM,
                key = it,
                isRequiredHexPrefixForTx = true,
            )
        }
    }

    private fun createTangemNewProvider(): ChiaNetworkProvider? {
        return config.chiaTangemApiKey?.letNotBlank {
            ChiaJsonRpcProvider(
                baseUrl = API_CHIA3_TANGEM,
                key = it,
                isRequiredHexPrefixForTx = true,
            )
        }
    }

    private fun createFireAcademyProvider(isTestnet: Boolean): ChiaNetworkProvider? {
        return config.chiaFireAcademyApiKey?.letNotBlank {
            ChiaJsonRpcProvider(
                baseUrl = if (isTestnet) API_CHIA_FIREACADEMY_TESTNET else API_CHIA_FIREACADEMY,
                key = config.chiaFireAcademyApiKey,
                isRequiredHexPrefixForTx = true,
            )
        }
    }
}