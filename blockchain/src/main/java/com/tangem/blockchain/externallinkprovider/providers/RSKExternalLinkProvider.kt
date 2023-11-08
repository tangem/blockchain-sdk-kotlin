package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class RSKExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://explorer.rsk.co/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return buildString {
            append(explorerBaseUrl + "address/$walletAddress")
            if (contractAddress != null) append("?__tab=tokens")
        }
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "tx/$transactionHash"
    }
}
