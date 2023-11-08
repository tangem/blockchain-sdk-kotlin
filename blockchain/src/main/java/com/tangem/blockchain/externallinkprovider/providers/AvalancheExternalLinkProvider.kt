package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class AvalancheExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) "https://testnet.snowtrace.io/" else "https://snowtrace.io/"

    override val testNetTopUpUrl: String? = if (isTestnet) "https://faucet.avax-test.network/" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return if (contractAddress != null) {
            explorerBaseUrl + "token/$contractAddress?a=$walletAddress"
        } else {
            explorerBaseUrl + "address/$walletAddress"
        }
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "tx/$transactionHash"
    }
}
