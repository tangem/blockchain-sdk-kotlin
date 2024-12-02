package com.tangem.blockchain.blockchains.factorn

import com.tangem.blockchain.blockchains.factorn.network.Fact0rnNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumNetworkProviderFactory

internal class Fact0rnProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<ElectrumNetworkProvider>(providerTypes) {

    override fun createProvider(url: String, blockchain: Blockchain): ElectrumNetworkProvider {
        return ElectrumNetworkProviderFactory.create(
            wssUrl = url,
            blockchain = blockchain,
            okHttpClient = BlockchainSdkRetrofitBuilder.build(),
            supportedProtocolVersion = Fact0rnNetworkService.SUPPORTED_SERVER_VERSION,
        )
    }
}