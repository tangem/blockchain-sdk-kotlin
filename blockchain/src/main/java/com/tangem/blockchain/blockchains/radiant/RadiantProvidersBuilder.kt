package com.tangem.blockchain.blockchains.radiant

import com.tangem.blockchain.blockchains.radiant.network.RadiantNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumNetworkProviderFactory

internal class RadiantProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<ElectrumNetworkProvider>(providerTypes) {

    override fun createProvider(url: String, blockchain: Blockchain): ElectrumNetworkProvider {
        return ElectrumNetworkProviderFactory.create(
            wssUrl = url,
            blockchain = blockchain,
            supportedProtocolVersion = RadiantNetworkService.SUPPORTED_SERVER_VERSION,
            okHttpClient = BlockchainSdkRetrofitBuilder.build(),
        )
    }
}