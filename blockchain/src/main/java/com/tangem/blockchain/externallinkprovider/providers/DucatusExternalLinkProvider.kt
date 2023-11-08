package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class DucatusExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://insight.ducatus.io/#/DUC/mainnet/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "tx/$transactionHash"
    }
}
