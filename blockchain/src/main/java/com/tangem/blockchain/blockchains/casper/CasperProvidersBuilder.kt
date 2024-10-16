package com.tangem.blockchain.blockchains.casper

import com.tangem.blockchain.blockchains.casper.network.CasperNetworkProvider
import com.tangem.blockchain.blockchains.casper.network.provider.CasperRpcNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.createWithPostfixIfContained
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class CasperProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<CasperNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<CasperNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> createPublicNetworkProvider(baseUrl = it.url)
                ProviderType.NowNodes -> createNowNodesNetworkProvider()
                else -> null
            }
        }
    }

    private fun createPublicNetworkProvider(baseUrl: String): CasperNetworkProvider {
        return createWithPostfixIfContained(
            baseUrl = baseUrl,
            postfixUrl = POSTFIX_URL,
            create = ::CasperRpcNetworkProvider,
        )
    }

    private fun createNowNodesNetworkProvider(): CasperNetworkProvider? {
        return config.nowNodeCredentials?.apiKey?.letNotBlank {
            CasperRpcNetworkProvider(
                baseUrl = "https://casper.nownodes.io/",
                postfixUrl = POSTFIX_URL,
                headerInterceptors = listOf(
                    AddHeaderInterceptor(mapOf(NowNodeCredentials.headerApiKey to it)),
                ),
            )
        }
    }

    private companion object {
        const val POSTFIX_URL = "rpc"
    }
}