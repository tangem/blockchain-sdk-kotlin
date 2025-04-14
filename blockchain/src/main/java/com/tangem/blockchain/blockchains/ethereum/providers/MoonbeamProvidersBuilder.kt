package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.createWithPostfixIfContained
import com.tangem.blockchain.common.network.providers.ProviderType

internal class MoonbeamProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> createPublicProvider(url = it.url)
                ProviderType.NowNodes -> {
                    ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://moonbeam.nownodes.io/")
                }
                ProviderType.GetBlock -> {
                    ethereumProviderFactory.getGetBlockProvider { moonbeam?.jsonRpc }
                }
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            "https://moonbase-alpha.public.blastapi.io/",
            "https://moonbase-rpc.dwellir.com/",
            "https://rpc.api.moonbase.moonbeam.network/",
            "https://moonbase.unitedbloc.com/",
            "https://moonbeam-alpha.api.onfinality.io/public/",
        )
            .map(::EthereumJsonRpcProvider)
    }

    private fun createPublicProvider(url: String): EthereumJsonRpcProvider {
        return createWithPostfixIfContained(
            baseUrl = url,
            postfixUrl = POSTFIX_URL,
            create = ::EthereumJsonRpcProvider,
        )
    }

    private companion object {
        const val POSTFIX_URL = "glmr"
    }
}