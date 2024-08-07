package com.tangem.blockchain.blockchains.filecoin

import com.tangem.blockchain.blockchains.filecoin.network.FilecoinNetworkProvider
import com.tangem.blockchain.blockchains.filecoin.network.provider.FilecoinRpcNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.createWithPostfixIfContained
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class FilecoinProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<FilecoinNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<FilecoinNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> createPublicNetworkProvider(baseUrl = it.url)
                ProviderType.NowNodes -> createNowNodesNetworkProvider()
                ProviderType.GetBlock -> createGetBlockNetworkProvider()
                else -> null
            }
        }
    }

    private fun createPublicNetworkProvider(baseUrl: String): FilecoinNetworkProvider {
        return createWithPostfixIfContained(
            baseUrl = baseUrl,
            postfixUrl = BASE_URL_LAST_PATH,
            create = ::FilecoinRpcNetworkProvider,
        )
    }

    private fun createNowNodesNetworkProvider(): FilecoinNetworkProvider? {
        return config.nowNodeCredentials?.apiKey?.letNotBlank {
            FilecoinRpcNetworkProvider(
                baseUrl = "https://fil.nownodes.io/",
                postfixUrl = BASE_URL_LAST_PATH,
                headerInterceptors = listOf(
                    AddHeaderInterceptor(mapOf(NowNodeCredentials.headerApiKey to it)),
                ),
            )
        }
    }

    private fun createGetBlockNetworkProvider(): FilecoinNetworkProvider? {
        return config.getBlockCredentials?.filecoin?.jsonRpc?.letNotBlank {
            FilecoinRpcNetworkProvider(baseUrl = "https://go.getblock.io/$it/", postfixUrl = BASE_URL_LAST_PATH)
        }
    }

    private companion object {
        const val BASE_URL_LAST_PATH = "rpc/v1"
    }
}