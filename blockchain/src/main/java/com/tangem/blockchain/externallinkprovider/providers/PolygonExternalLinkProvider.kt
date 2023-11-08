package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class PolygonExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://mumbai.polygonscan.com/" else "https://polygonscan.com/"

    override val testNetTopUpUrl: String? = if (isTestnet) "https://faucet.matic.network" else null

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
