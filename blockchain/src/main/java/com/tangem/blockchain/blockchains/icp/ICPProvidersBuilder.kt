package com.tangem.blockchain.blockchains.icp

import com.tangem.blockchain.blockchains.icp.network.ICPIc4jAgentNetworkProvider
import com.tangem.blockchain.blockchains.icp.network.ICPNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class ICPProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    val walletPublicKey: Wallet.PublicKey,
) : OnlyPublicProvidersBuilder<ICPNetworkProvider>(providerTypes) {

    override fun createProvider(url: String, blockchain: Blockchain): ICPNetworkProvider =
        ICPIc4jAgentNetworkProvider(url, walletPublicKey)
}