package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class HederaExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) TODO() else TODO()

    override val testNetTopUpUrl: String? = if (isTestnet) null else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + TODO()
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + TODO()
    }
}
