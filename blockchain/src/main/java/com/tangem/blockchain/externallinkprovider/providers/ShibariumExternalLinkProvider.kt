package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class ShibariumExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://puppyscan.shib.io/" else "https://www.shibariumscan.io/"

    override val testNetTopUpUrl: String = "https://beta.shibariumtech.com/faucet/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return "${explorerBaseUrl}tx/$transactionHash"
    }
}