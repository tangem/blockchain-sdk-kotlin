package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class TerraV1ExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://atomscan.com/terra/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "accounts/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "transactions/$transactionHash"
    }
}
