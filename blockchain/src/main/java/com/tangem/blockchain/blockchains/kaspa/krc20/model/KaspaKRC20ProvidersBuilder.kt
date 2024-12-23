package com.tangem.blockchain.blockchains.kaspa.krc20.model

import com.tangem.blockchain.blockchains.kaspa.krc20.KaspaKRC20NetworkProvider
import com.tangem.blockchain.blockchains.kaspa.krc20.KaspaKRC20RestApiNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class KaspaKRC20ProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : NetworkProvidersBuilder<KaspaKRC20NetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<KaspaKRC20NetworkProvider> {
        return listOf(KaspaKRC20RestApiNetworkProvider("https://api.kasplex.org/v1/"))
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<KaspaKRC20NetworkProvider> {
        return listOf(KaspaKRC20RestApiNetworkProvider("https://tn10api.kasplex.org/v1"))
    }
}