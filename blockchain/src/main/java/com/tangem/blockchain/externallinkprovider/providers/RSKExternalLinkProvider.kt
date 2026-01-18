package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class RSKExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://explorer.rsk.co/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return buildString {
            append(explorerBaseUrl + "address/$walletAddress")
            if (contractAddress != null) append("?__tab=tokens")
        }
    }

    override fun getExplorerTxUrl(transactionHash: String, contractAddress: String?): TxExploreState {
        return TxExploreState.Url(explorerBaseUrl + "tx/$transactionHash")
    }
}