package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class CardanoExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://www.blockchair.com/cardano/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "transaction/$transactionHash"
    }
}
