package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class XDCExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) {
        "https://explorer.apothem.network/"
    } else {
        "https://explorer.xinfin.network/"
    }

    override val testNetTopUpUrl: String = "https://faucet.apothem.network/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "txs/$transactionHash"
    }
}
