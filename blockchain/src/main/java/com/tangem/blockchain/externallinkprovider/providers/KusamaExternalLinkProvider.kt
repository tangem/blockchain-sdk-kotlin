package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class KusamaExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://kusama.subscan.io/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "account/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "extrinsic/$transactionHash"
    }
}
