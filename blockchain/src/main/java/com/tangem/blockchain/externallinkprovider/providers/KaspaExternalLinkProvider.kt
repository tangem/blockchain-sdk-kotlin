package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class KaspaExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://explorer.kaspa.org/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "addresses/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "txs/$transactionHash"
    }
}
