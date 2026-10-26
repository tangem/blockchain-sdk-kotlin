package com.tangem.blockchain.blockchains.sui

import com.tangem.blockchain.blockchains.sui.network.grpc.SuiGrpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class SuiNetworkProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<SuiGrpcProvider>() {

    override fun createProviders(blockchain: Blockchain): List<SuiGrpcProvider> {
        return providerTypes.mapNotNull { type ->
            when (type) {
                is ProviderType.Public -> SuiGrpcProvider(baseUrl = type.url)
                is ProviderType.GetBlock -> createGetBlockProvider()
                is ProviderType.NowNodes -> createNowNodesProvider()
                ProviderType.QuickNode -> createQuickNodeProvider()
                else -> null
            }
        }
    }

    private fun createGetBlockProvider(): SuiGrpcProvider? {
        return config.getBlockCredentials?.sui?.grpc.letNotBlank { token ->
            SuiGrpcProvider(
                baseUrl = "https://shared.us-east-1.getblock.io/",
                headers = mapOf("x-access-token" to token),
            )
        }
    }

    private fun createNowNodesProvider(): SuiGrpcProvider? {
        return config.nowNodeCredentials?.apiKey.letNotBlank { apiKey ->
            SuiGrpcProvider(
                baseUrl = "https://sui-grpc.nownodes.io/",
                headers = mapOf("api-key" to apiKey),
            )
        }
    }

    private fun createQuickNodeProvider(): SuiGrpcProvider? {
        val credentials = config.quickNodeSuiCredentials ?: return null
        if (credentials.subdomain.isBlank() || credentials.apiKey.isBlank()) return null

        return SuiGrpcProvider(
            baseUrl = "https://${credentials.subdomain}/",
            headers = mapOf("x-token" to credentials.apiKey),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<SuiGrpcProvider> {
        return listOf(
            SuiGrpcProvider(baseUrl = "https://fullnode.testnet.sui.io/"),
        )
    }
}