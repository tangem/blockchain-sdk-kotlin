package com.tangem.blockchain.blockchains.xrp

import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.blockchains.xrp.network.rippled.RippledNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_XRP_LEDGER_FOUNDATION

internal class XRPProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<XrpNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.XRP)

    override fun createProviders(blockchain: Blockchain): List<XrpNetworkProvider> {
        return listOfNotNull(
            RippledNetworkProvider(API_XRP_LEDGER_FOUNDATION),
            config.nowNodeCredentials?.apiKey?.letNotBlank { apiKey ->
                RippledNetworkProvider(
                    baseUrl = "https://xrp.nownodes.io/",
                    apiKeyHeader = NowNodeCredentials.headerApiKey to apiKey,
                )
            },
            config.getBlockCredentials?.xrp?.jsonRpc.letNotBlank { jsonRpcToken ->
                RippledNetworkProvider(
                    baseUrl = "https://go.getblock.io/$jsonRpcToken/",
                )
            },
        )
    }
}