package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class AlephZeroExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://alephzero.subscan.io/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "account/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "extrinsic/$transactionHash"
    }
}
