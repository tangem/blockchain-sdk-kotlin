package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class TezosExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://tzkt.io/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "$explorerBaseUrl$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "tx/$transactionHash"
    }
}
