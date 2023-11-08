package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class LitecoinExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://blockchair.com/litecoin/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "transaction/$transactionHash"
    }
}
